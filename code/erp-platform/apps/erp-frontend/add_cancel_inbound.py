import re

with open('src/components/inventory/InboundReceiptForm.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

import_str = "import { ConfirmDialog } from '../../shared/components/Dialog/ConfirmDialog';\nimport { useTabs } from '../../context/TabContext';\n"
content = content.replace("import toast from 'react-hot-toast';", import_str + "import toast from 'react-hot-toast';")

content = content.replace("const { user } = useAuth();", "const { user } = useAuth();\n    const { closeTab } = useTabs();\n    const [showCancelConfirm, setShowCancelConfirm] = useState(false);")

handle_cancel_old = '''handleCancel: () => {
        if (receiptId) {
          setCurrentMode('VIEW');
        } else {
          // close tab logic in parent
        }
      },'''
handle_cancel_new = '''handleCancel: () => {
        if (currentMode === 'ADD' || currentMode === 'EDIT') {
          setShowCancelConfirm(true);
        }
      },'''
content = content.replace(handle_cancel_old, handle_cancel_new)

confirm_dialog = '''
        <ConfirmDialog
          isOpen={showCancelConfirm}
          title="Xác nh?n h?y"
          message="B?n có ch?c ch?n mu?n h?y phi?u nh?p này? Các thay d?i s? không du?c luu."
          confirmLabel="Ð?ng ý"
          cancelLabel="Ðóng"
          onConfirm={() => {
            setShowCancelConfirm(false);
            if (receiptId) setCurrentMode('VIEW');
            else closeTab('inbound'); // Assuming 'inbound' is the tab ID, we'll try to find parent or just generic
          }}
          onCancel={() => setShowCancelConfirm(false)}
        />
      </div>
    );
'''
content = content.replace("</div>\n    );\n  }\n);", confirm_dialog + "  }\n);")

with open('src/components/inventory/InboundReceiptForm.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

