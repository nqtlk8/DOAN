import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { GenericDocumentForm } from '../GenericDocumentForm';

describe('GenericDocumentForm', () => {
  const defaultProps = {
    mode: 'VIEW' as const,
    docTitle: 'Test Form',
    docCode: 'TEST-001',
    info: [],
    createdDate: '2026-01-01',
    partner: {
      label: 'Partner',
      displayName: 'Test Partner',
      renderCombobox: () => <div data-testid="combo" />,
      fields: []
    },
    summary: [],
    lines: {
      testIdPrefix: 'test' as any,
      items: [],
      onAdd: vi.fn(),
      onRemove: vi.fn(),
      onUpdate: vi.fn(),
      renderProductCombobox: () => <div />
    }
  };

  it('renders title and code', () => {
    render(<GenericDocumentForm {...defaultProps} />);
    expect(screen.getByText('Test Form')).toBeInTheDocument();
    expect(screen.getByTestId('doc-code')).toHaveTextContent('TEST-001');
  });

  it('renders footer buttons based on props', () => {
    const onAdd = vi.fn();
    render(<GenericDocumentForm {...defaultProps} onAdd={onAdd} />);
    expect(screen.getByTestId('btn-add')).toBeInTheDocument();
  });
});
