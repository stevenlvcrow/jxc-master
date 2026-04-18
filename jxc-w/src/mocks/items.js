const names = ['安佳黄油', '高筋面粉', '冷冻鸡胸肉', '黑糖珍珠', '海天生抽', '一次性打包盒', '冻南美白虾', '河粉', '藤椒鸡预制包', '乌龙茶底', '生菜', '猪五花', '香辣卤牛肉', '纸巾', '手抓饼胚', '拉面', '可乐', '前厅托盘', '冻龙利鱼', '餐具四件套'];
const specs = ['1kg/包', '2kg/箱', '500g/袋', '24包/箱', '800g/盒', '1.9L/瓶'];
const categories = ['蔬菜', '奶茶', '肉类', '调料', '面点', '熟食', '日用百货', '水产', '河粉', '面食', '预制菜', '酒水', '一次性用品', '前厅类'];
const brands = ['安佳', '金像', '凤祥', '味全', '海天', '洁美', '海之味', '穗香', '川味源', '茶里', '田园鲜', '双汇', '卤香坊'];
const statTypes = ['原料类 (成本类)', '酒水类 (成本类)', '调料料类 (成本类)', '半成品类 (成本类)', '低值易耗品类 (费用类)', '固定资产类 (费用类)'];
const storageModes = ['冷藏', '冷冻', '常温'];
const itemTypes = ['普通物品', '套餐'];
const abcList = ['A', 'B', 'C'];
const pad = (value, len = 6) => String(value).padStart(len, '0');
const makeDate = (index) => {
    const day = ((index % 28) + 1).toString().padStart(2, '0');
    const hour = ((index * 3) % 24).toString().padStart(2, '0');
    const minute = ((index * 7) % 60).toString().padStart(2, '0');
    return `2026-04-${day} ${hour}:${minute}:00`;
};
const createItem = (index) => {
    const status = index % 5 === 0 ? '停用' : '启用';
    const itemType = itemTypes[index % itemTypes.length];
    const base = (index % 20) + 1;
    return {
        id: `itm-${pad(index, 4)}`,
        index,
        code: `SP${pad(index)}`,
        name: `${names[index % names.length]}${Math.ceil(index / names.length)}`,
        spec: specs[index % specs.length],
        type: itemType,
        category: categories[index % categories.length],
        brand: brands[index % brands.length],
        productionCost: (8 + base * 1.35).toFixed(2),
        baseUnit: ['kg', '箱', '袋', '瓶', '包'][index % 5],
        purchaseUnit: ['箱', '袋', '桶'][index % 3],
        orderUnit: ['箱', '袋', '桶'][index % 3],
        stockUnit: ['kg', '箱', '袋', '瓶', '包'][index % 5],
        costUnit: ['kg', '箱', '袋', '瓶', '包'][index % 5],
        suggestPrice: (9 + base * 1.42).toFixed(2),
        volume: (0.001 + (index % 8) * 0.002).toFixed(3),
        weight: (0.2 + (index % 15) * 0.3).toFixed(3),
        statType: statTypes[index % statTypes.length],
        thirdPartyCode: `THD-${pad(index, 4)}`,
        abcClass: abcList[index % abcList.length],
        stockMin: String(5 + (index % 20)),
        stockMax: String(60 + (index % 80) * 2),
        source: index % 2 === 0 ? '自建' : '导入',
        status,
        storageMode: storageModes[index % storageModes.length],
        tag: ['热销', '新品', '临期', '大包装', '常备'][index % 5],
        image: index % 3 === 0 ? '已上传' : '未上传',
        createdAt: makeDate(index),
        updatedAt: makeDate(index + 30),
    };
};
let mockItemDb = Array.from({ length: 180 }, (_, idx) => createItem(idx + 1));
const toLower = (value) => value.toLowerCase();
const filterItems = (items, params) => {
    const keyword = params.keyword?.trim();
    return items.filter((item) => {
        if (keyword) {
            const text = toLower(keyword);
            const joined = toLower(`${item.code} ${item.name} ${item.spec} ${item.thirdPartyCode}`);
            if (!joined.includes(text)) {
                return false;
            }
        }
        if (params.status && params.status !== '全部' && item.status !== params.status) {
            return false;
        }
        if (params.itemType && params.itemType !== '全部' && item.type !== params.itemType) {
            return false;
        }
        if (params.statType && params.statType !== '全部' && item.statType !== params.statType) {
            return false;
        }
        if (params.storageMode && item.storageMode !== params.storageMode) {
            return false;
        }
        if (params.tag?.trim() && !item.tag.includes(params.tag.trim())) {
            return false;
        }
        return true;
    });
};
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
export const mockFetchItems = async (params) => {
    await sleep(240);
    const pageNo = Number(params.pageNo || 1);
    const pageSize = Number(params.pageSize || 10);
    const filtered = filterItems(mockItemDb, params);
    const start = (pageNo - 1) * pageSize;
    const end = start + pageSize;
    const list = filtered.slice(start, end).map((item, idx) => ({
        ...item,
        index: start + idx + 1,
    }));
    return {
        list,
        total: filtered.length,
        pageNo,
        pageSize,
    };
};
export const mockBatchUpdateStatus = async (ids, status) => {
    await sleep(180);
    const idSet = new Set(ids);
    mockItemDb = mockItemDb.map((item) => (idSet.has(item.id) ? { ...item, status } : item));
};
export const mockBatchDeleteItems = async (ids) => {
    await sleep(180);
    const idSet = new Set(ids);
    mockItemDb = mockItemDb.filter((item) => !idSet.has(item.id));
};
