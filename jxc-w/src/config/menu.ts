export type AppMenuItem = {
  key: string;
  title: string;
  menuCode?: string;
  path?: string;
  icon?: string;
  componentKey?: string;
  menuType?: 'DIRECTORY' | 'MENU';
  children?: AppMenuItem[];
};
