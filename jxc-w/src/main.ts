import { createApp } from 'vue';

import App from './App.vue';
import router from './router';
import { installElementPlus } from './plugins/element-plus';
import '@/styles/index.scss';
import { pinia } from '@/stores';

const app = createApp(App);

app.use(pinia);
app.use(router);
installElementPlus(app);

app.mount('#app');
