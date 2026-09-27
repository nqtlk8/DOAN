import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MdiModuleLayout } from '../MdiModuleLayout';

describe('MdiModuleLayout', () => {
  it('renders children correctly', () => {
    render(
      <MdiModuleLayout activeSubView="FORM" onSubViewChange={vi.fn()}>
        <div>Test Content</div>
      </MdiModuleLayout>
    );
    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('renders buttons based on props', () => {
    const onAdd = vi.fn();
    render(
      <MdiModuleLayout activeSubView="FORM" onSubViewChange={vi.fn()} onAdd={onAdd}>
        <div />
      </MdiModuleLayout>
    );
    expect(screen.getByTestId('btn-add')).toBeInTheDocument();
  });
});
