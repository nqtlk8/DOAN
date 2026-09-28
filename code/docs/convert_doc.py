import re
import os
import zlib
import base64
import requests
from docx import Document
from docx.shared import Pt, Cm, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_ORIENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

def get_kroki_url(diagram_type, diagram_text):
    data = diagram_text.encode('utf-8')
    compressed = zlib.compress(data, 9)
    encoded = base64.urlsafe_b64encode(compressed).decode('ascii')
    return f"https://kroki.io/{diagram_type}/png/{encoded}"

def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(text)
    run.font.name = 'Times New Roman'
    run.font.size = Pt(13)
    run.font.italic = True
    run.font.color.rgb = RGBColor(0, 0, 255)

def set_page_margins(section, top_cm, bottom_cm, left_cm, right_cm):
    section.top_margin = Cm(top_cm)
    section.bottom_margin = Cm(bottom_cm)
    section.left_margin = Cm(left_cm)
    section.right_margin = Cm(right_cm)

def insert_toc(doc):
    p = doc.add_paragraph()
    run = p.add_run()
    fldChar1 = OxmlElement('w:fldChar')
    fldChar1.set(qn('w:fldCharType'), 'begin')
    
    instrText = OxmlElement('w:instrText')
    instrText.set(qn('xml:space'), 'preserve')
    instrText.text = 'TOC \\o "1-3" \\h \\z \\u'
    
    fldChar2 = OxmlElement('w:fldChar')
    fldChar2.set(qn('w:fldCharType'), 'separate')
    
    fldChar3 = OxmlElement('w:fldChar')
    fldChar3.set(qn('w:fldCharType'), 'end')

    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    run._r.append(fldChar3)

def add_table_borders(table):
    tbl = table._tbl
    tblPr = tbl.tblPr
    if tblPr is None:
        tblPr = OxmlElement('w:tblPr')
        tbl.insert(0, tblPr)
        
    tblBorders = OxmlElement('w:tblBorders')
    
    for border_name in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), 'single')
        border.set(qn('w:sz'), '4')
        border.set(qn('w:space'), '0')
        border.set(qn('w:color'), '000000')
        tblBorders.append(border)
        
    tblPr.append(tblBorders)


def process_markdown(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    doc = Document()
    
    style = doc.styles['Normal']
    font = style.font
    font.name = 'Times New Roman'
    font.size = Pt(13)
    p_format = style.paragraph_format
    p_format.line_spacing = 1.5
    
    section = doc.sections[0]
    set_page_margins(section, 3, 3, 3, 2)
    
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("TRƯỜNG ĐẠI HỌC KINH TẾ TP.HCM\nKhoa Công nghệ Thông tin Kinh doanh\n\n")
    r.font.size = Pt(14)
    r.font.bold = True
    
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("BÁO CÁO KHÓA LUẬN TỐT NGHIỆP\n\nĐỀ TÀI:\nXÂY DỰNG HỆ THỐNG ERP ĐA CHI NHÁNH TRONG LĨNH VỰC BÁN LẺ VẬT LIỆU XÂY DỰNG\n\n")
    r.font.size = Pt(18)
    r.font.bold = True
    
    p = doc.add_paragraph("Sinh viên thực hiện: Nguyễn Văn A\nGiảng viên hướng dẫn: TS. Nguyễn Văn B")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    doc.add_page_break()
    
    doc.add_heading("Lời cảm ơn", level=1)
    doc.add_paragraph("Em xin chân thành cảm ơn...")
    doc.add_page_break()
    
    doc.add_heading("Mục lục", level=1)
    insert_toc(doc)
    doc.add_page_break()

    doc.add_heading("Danh sách chữ viết tắt, ký hiệu", level=1)
    doc.add_paragraph("Ký hiệu 1: ...")
    doc.add_page_break()

    doc.add_heading("Danh sách sơ đồ/hình ảnh", level=1)
    # Just a text placeholder since TOC for figures needs a custom field code
    p = doc.add_paragraph()
    run = p.add_run()
    fldChar1 = OxmlElement('w:fldChar')
    fldChar1.set(qn('w:fldCharType'), 'begin')
    instrText = OxmlElement('w:instrText')
    instrText.set(qn('xml:space'), 'preserve')
    instrText.text = 'TOC \\h \\z \\c "Hình"'
    fldChar2 = OxmlElement('w:fldChar')
    fldChar2.set(qn('w:fldCharType'), 'separate')
    fldChar3 = OxmlElement('w:fldChar')
    fldChar3.set(qn('w:fldCharType'), 'end')
    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    run._r.append(fldChar3)
    doc.add_page_break()

    in_code_block = False
    code_type = None
    code_content = []
    
    in_table = False
    table_rows = []
    
    i = 0
    img_counter = 0
    while i < len(lines):
        line = lines[i].rstrip('\n')
        
        if line.startswith('```'):
            if in_code_block:
                in_code_block = False
                diagram_text = "\n".join(code_content)
                code_content = []
                print(f"Generating diagram {code_type}...")
                dtype = 'plantuml' if 'plantuml' in code_type else 'mermaid' if 'mermaid' in code_type else code_type
                if dtype in ['plantuml', 'mermaid']:
                    url = get_kroki_url(dtype, diagram_text)
                    resp = requests.get(url)
                    if resp.status_code == 200:
                        img_path = f"img_{img_counter}.png"
                        with open(img_path, 'wb') as f:
                            f.write(resp.content)
                        img_counter += 1
                        
                        is_landscape = False
                        if len(diagram_text.split('\n')) > 40: 
                            is_landscape = True
                            
                        if is_landscape:
                            new_section = doc.add_section()
                            new_section.orientation = WD_ORIENT.LANDSCAPE
                            new_section.page_width, new_section.page_height = new_section.page_height, new_section.page_width
                            set_page_margins(new_section, 3, 3, 3, 2)
                        
                        p = doc.add_paragraph()
                        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                        p.add_run().add_picture(img_path, width=Inches(6) if not is_landscape else Inches(9))
                        
                        if is_landscape:
                            new_section = doc.add_section()
                            new_section.orientation = WD_ORIENT.PORTRAIT
                            new_section.page_width, new_section.page_height = new_section.page_height, new_section.page_width
                            set_page_margins(new_section, 3, 3, 3, 2)

                else:
                    doc.add_paragraph(diagram_text, style='No Spacing')
            else:
                in_code_block = True
                code_type = line[3:].strip()
                code_content = []
            i += 1
            continue
            
        if in_code_block:
            code_content.append(line)
            i += 1
            continue
            
        if line.strip().startswith('|') and line.strip().endswith('|'):
            in_table = True
            row = [cell.strip() for cell in line.strip().split('|')[1:-1]]
            if not all(c == '-' or c == '' or c == ':' for c in row[0].replace('-', '')):
                table_rows.append(row)
            i += 1
            continue
            
        if in_table:
            in_table = False
            num_cols = max(len(r) for r in table_rows)
            is_wide_table = num_cols > 6
            
            if is_wide_table:
                new_section = doc.add_section()
                new_section.orientation = WD_ORIENT.LANDSCAPE
                new_section.page_width, new_section.page_height = new_section.page_height, new_section.page_width
                set_page_margins(new_section, 3, 3, 3, 2)
            
            table = doc.add_table(rows=len(table_rows), cols=num_cols)
            add_table_borders(table)
            
            for r_idx, row in enumerate(table_rows):
                for c_idx, cell in enumerate(row):
                    if c_idx < num_cols:
                        cell_obj = table.cell(r_idx, c_idx)
                        cell_obj.text = cell
                        if r_idx == 0:
                            for paragraph in cell_obj.paragraphs:
                                for run in paragraph.runs:
                                    run.font.bold = True
            
            if is_wide_table:
                new_section = doc.add_section()
                new_section.orientation = WD_ORIENT.PORTRAIT
                new_section.page_width, new_section.page_height = new_section.page_height, new_section.page_width
                set_page_margins(new_section, 3, 3, 3, 2)
            
            table_rows = []

        if line.startswith('*Hình'):
            caption_text = line.replace('*', '').strip()
            add_caption(doc, caption_text)
            i += 1
            continue
            
        if line.startswith('#'):
            level = len(line.split(' ')[0])
            text = line[level:].strip()
            if level <= 4:
                doc.add_heading(text, level=level)
            i += 1
            continue
            
        if line.strip() != "":
            clean_line = re.sub(r'\*\*(.*?)\*\*', r'\1', line)
            clean_line = re.sub(r'\*(.*?)\*', r'\1', clean_line)
            
            p = doc.add_paragraph(clean_line)
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            
        i += 1
        
    doc.save('BAOCAO_HOANCHINH.docx')
    print("Done generating DOCX.")

if __name__ == "__main__":
    process_markdown("BAOCAO_HOANCHINH_V2.md")
