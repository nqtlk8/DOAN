/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ENFORCE_BRANCH_URL?: string;
  // add more env variables here if needed
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
