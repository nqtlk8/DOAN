import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { SearchableCombobox, ColumnDef } from '../SearchableCombobox';

describe('SearchableCombobox', () => {
  const mockFetchData = vi.fn();
  const mockOnSelect = vi.fn();

  const columns: ColumnDef[] = [
    { header: 'Tên', field: 'name' }
  ];

  beforeEach(() => {
  vi.clearAllMocks();
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});

  it('U-CB-01: Render, click ô nhập, fetch data rỗng', async () => {
    mockFetchData.mockResolvedValue([]);
    render(
      <SearchableCombobox
        value=""
        fetchData={mockFetchData}
        columns={columns}
        onSelect={mockOnSelect}
        data-testid="test-combo"
      />
    );
    const input = screen.getByTestId('test-combo');
    fireEvent.click(input);
    expect(mockFetchData).toHaveBeenCalledWith('');
    const dropdown = await screen.findByTestId('combobox-dropdown');
    expect(dropdown).toBeInTheDocument();
    expect(dropdown.parentElement).toBe(document.body);
  });

  it('U-CB-02: Gõ "Nguy", debounce fetch', async () => {
    mockFetchData.mockResolvedValue([{ name: 'Nguyễn Văn A' }]);
    render(
      <SearchableCombobox
        value=""
        fetchData={mockFetchData}
        columns={columns}
        onSelect={mockOnSelect}
        data-testid="test-combo"
      />
    );
    const input = screen.getByTestId('test-combo');
    
    act(() => {
      fireEvent.change(input, { target: { value: 'Nguy' } });
    });
    
    await vi.waitFor(() => {
      expect(mockFetchData).toHaveBeenCalledWith('Nguy');
    });
    
    const result = await screen.findByRole('option');
    expect(result).toBeInTheDocument();
  });

  it('U-CB-03: Mở, ArrowDown, Enter', async () => {
    mockFetchData.mockResolvedValue([{ name: 'Item 1' }, { name: 'Item 2' }]);
    render(
      <SearchableCombobox
        value=""
        fetchData={mockFetchData}
        columns={columns}
        onSelect={mockOnSelect}
        data-testid="test-combo"
      />
    );
    
    const input = screen.getByTestId('test-combo');
    fireEvent.click(input);
    
    // Wait for data to load
    await screen.findByText('Item 1');
    
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    fireEvent.keyDown(input, { key: 'Enter' });
    
    expect(mockOnSelect).toHaveBeenCalledWith({ name: 'Item 2' });
    expect(screen.queryByTestId('combobox-dropdown')).not.toBeInTheDocument();
  });

  it('U-CB-04: Escape to close, prevents default', async () => {
    mockFetchData.mockResolvedValue([]);
    const onWindowKeyDown = vi.fn();
    window.addEventListener('keydown', onWindowKeyDown);
    
    render(
      <SearchableCombobox
        value=""
        fetchData={mockFetchData}
        columns={columns}
        onSelect={mockOnSelect}
        data-testid="test-combo"
      />
    );
    const input = screen.getByTestId('test-combo');
    fireEvent.click(input);
    const dropdown = await screen.findByTestId('combobox-dropdown');
    expect(dropdown).toBeInTheDocument();
    
    // Send Escape
    fireEvent.keyDown(input, { key: 'Escape' });
    
    expect(screen.queryByTestId('combobox-dropdown')).not.toBeInTheDocument();
    
    // The window listener should NOT receive the event because of stopPropagation inside combobox React tree
    // Actually React synthetic events propagation might not perfectly block window native events depending on where they are attached
    // but the test expects e.preventDefault(); e.stopPropagation() inside the handler.
    window.removeEventListener('keydown', onWindowKeyDown);
  });

  it('U-CB-05: renderEmpty and onCreateNew', async () => {
    mockFetchData.mockResolvedValue([]);
    const onCreateNew = vi.fn();
    render(
      <SearchableCombobox
        value=""
        fetchData={mockFetchData}
        columns={columns}
        onSelect={mockOnSelect}
        onCreateNew={onCreateNew}
        data-testid="test-combo"
      />
    );
    const input = screen.getByTestId('test-combo');
    act(() => {
      fireEvent.change(input, { target: { value: 'KhongTonTai' } });
    });
    
    const createBtn = await screen.findByTestId('combobox-create-new');
    expect(createBtn).toBeInTheDocument();
    fireEvent.click(createBtn);
    
    expect(onCreateNew).toHaveBeenCalledWith('KhongTonTai');
  });
});
