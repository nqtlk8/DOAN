import re

with open('src/components/layout/TopRibbon.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

import_str = "import { GoodsReturnModule } from '../inventory/GoodsReturnModule';\n"
content = content.replace("import { InboundReceiptModule } from '../inventory/InboundReceiptModule';", import_str + "import { InboundReceiptModule } from '../inventory/InboundReceiptModule';")

button_str = '''
            <button 
              data-testid="ribbon-btn-return"
              onClick={() => handleOpenTab('goods-return', 'NH?P L?I HÀNG BÁN', <GoodsReturnModule mode="ADD" />, false)}
              className="flex flex-col items-center justify-center p-1 min-w-[60px] shrink-0 hover:bg-primary-soft rounded-[4px] gap-1"
            >
              <Truck size={20} className="text-erp-text-accent-orange" />
              <span className="text-[12px] whitespace-nowrap leading-none text-ink">Nh?p L?i Hàng Bán</span>
            </button>
'''

target = '''<span className="text-[12px] whitespace-nowrap leading-none text-ink">Nh?p Hàng</span>
            </button>'''

content = content.replace(target, target + button_str)

with open('src/components/layout/TopRibbon.tsx', 'w', encoding='utf-8') as f:
    f.write(content)
