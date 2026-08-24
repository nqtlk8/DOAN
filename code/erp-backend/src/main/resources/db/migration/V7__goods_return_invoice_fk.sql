-- V7 Migration: Add Foreign Key for goods_return to sales_invoice
ALTER TABLE goods_return
ADD CONSTRAINT fk_gr_invoice FOREIGN KEY (invoice_id) REFERENCES sales_invoice(id);
