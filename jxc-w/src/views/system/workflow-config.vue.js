/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { fetchWorkflowProcessesApi, fetchCurrentWorkflowConfigApi, saveCurrentWorkflowConfigApi, } from '@/api/modules/workflow';
import { fetchAdminRolesApi, fetchAdminUsersApi } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
const sessionStore = useSessionStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const businessLoading = ref(false);
const approverLoading = ref(false);
const workflowBusinesses = ref([]);
const roleOptions = ref([]);
const userOptions = ref([]);
const TARGET_BUSINESS_CODE = 'PURCHASE_INBOUND';
const TARGET_BUSINESS_NAME = '采购入库流程';
const currentOrgId = computed(() => sessionStore.currentOrgId || undefined);
const copySourceWorkflowCode = ref('');
const viewWorkflowCode = ref('');
const isReadOnlyMode = computed(() => Boolean(viewWorkflowCode.value));
const selectedNodeKey = ref('');
const canvasRef = ref(null);
const canvasSize = reactive({ width: 1200, height: 560 });
const canvasContentSize = reactive({ width: 1600, height: 960 });
let canvasResizeObserver = null;
let lastCanvasWidth = 0;
const connectingMode = ref(false);
const connectingFromKey = ref('');
const edges = ref([]);
const addDialogVisible = ref(false);
const editDialogVisible = ref(false);
const addAfterNodeKey = ref('');
const contextMenu = reactive({
    visible: false,
    nodeKey: '',
});
const ACTION_OPTIONS = [
    { label: '新增', value: 'CREATE' },
    { label: '修改', value: 'UPDATE' },
    { label: '删除', value: 'DELETE' },
];
const NODE_TYPE_LABEL = {
    NORMAL: '普通节点',
    CONDITION: '条件节点',
    SUCCESS: '成功节点',
    FAIL: '失败节点',
    START: '开始节点',
    END: '结束节点',
};
const normalizeNodeTypeValue = (value) => {
    if (value === 'CONDITION' || value === 'SUCCESS' || value === 'FAIL' || value === 'START' || value === 'END' || value === 'NORMAL') {
        return value;
    }
    return 'NORMAL';
};
const normalizeRoleSignMode = (value) => (value === 'AND' ? 'AND' : 'OR');
const supportsRoleAssignment = (type) => {
    const nodeType = normalizeNodeTypeValue(type);
    return nodeType === 'NORMAL' || nodeType === 'CONDITION';
};
const supportsActionTriggers = (type) => normalizeNodeTypeValue(type) === 'NORMAL';
const supportsApproverUser = (type) => normalizeNodeTypeValue(type) === 'CONDITION';
const nodeDraft = reactive({
    nodeType: 'NORMAL',
    nodeName: '',
    allowUnapprove: true,
    approverRoleCode: '',
    roleSignMode: 'OR',
    approverUserId: undefined,
    triggerActions: [],
});
const dragState = reactive({
    nodeKey: '',
    offsetX: 0,
    offsetY: 0,
    active: false,
});
const currentBusinessCode = ref('');
const form = reactive({
    scopeType: '',
    businessCode: '',
    workflowCode: '',
    workflowName: '',
    deployedAt: '',
    nodes: [],
});
const isScopedContext = computed(() => sessionStore.currentOrgId.startsWith('group-') || sessionStore.currentOrgId.startsWith('store-'));
const businessOptions = computed(() => workflowBusinesses.value.map((item) => ({
    label: item.businessName,
    value: item.processId,
})).filter((item) => item.value === TARGET_BUSINESS_CODE));
const currentBusiness = computed(() => workflowBusinesses.value.find((item) => item.processId === currentBusinessCode.value) ?? null);
const selectedNode = computed(() => form.nodes.find((item) => item.nodeKey === selectedNodeKey.value) ?? null);
const contextMenuNode = computed(() => form.nodes.find((item) => item.nodeKey === contextMenu.nodeKey) ?? null);
const rollbackTargetNodes = computed(() => {
    const source = contextMenuNode.value;
    if (!source || normalizeNodeTypeValue(source.nodeType) !== 'FAIL') {
        return [];
    }
    const sourceIndex = form.nodes.findIndex((item) => item.nodeKey === source.nodeKey);
    return form.nodes.filter((item, index) => {
        if (item.nodeKey === source.nodeKey) {
            return false;
        }
        const type = normalizeNodeTypeValue(item.nodeType);
        if (type !== 'NORMAL' && type !== 'END') {
            return false;
        }
        return index < sourceIndex || item.x < source.x;
    });
});
const editDraft = reactive({
    nodeKey: '',
    nodeName: '',
    nodeType: 'NORMAL',
    allowUnapprove: true,
    approverRoleCode: '',
    roleSignMode: 'OR',
    approverUserId: undefined,
    triggerActions: [],
});
const applyBusinessSelection = (businessCode) => {
    if (businessCode !== TARGET_BUSINESS_CODE) {
        currentBusinessCode.value = '';
        form.businessCode = '';
        form.workflowName = '';
        form.workflowCode = '';
        form.deployedAt = '';
        return;
    }
    currentBusinessCode.value = businessCode;
    const selected = workflowBusinesses.value.find((item) => item.processId === businessCode) ?? null;
    form.businessCode = selected?.processId ?? '';
    form.workflowName = selected?.businessName ?? '';
    form.workflowCode = selected?.templateId?.trim() ?? '';
    form.deployedAt = '';
};
const normalizeNode = (node, index) => {
    const nodeType = normalizeNodeTypeValue(node.nodeType);
    const allowUnapprove = nodeType === 'SUCCESS' ? node.allowUnapprove !== false : false;
    return {
        nodeKey: node.nodeKey || `node_${index + 1}`,
        nodeName: node.nodeName || `${NODE_TYPE_LABEL[nodeType]}${index + 1}`,
        allowReject: false,
        allowUnapprove,
        nodeType,
        approverRoleCode: supportsRoleAssignment(nodeType) ? String(node.approverRoleCode ?? '').trim() : '',
        roleSignMode: nodeType === 'CONDITION' ? normalizeRoleSignMode(node.roleSignMode) : 'OR',
        approverUserId: supportsApproverUser(nodeType) && typeof node.approverUserId === 'number' ? node.approverUserId : undefined,
        conditionExpression: (node.conditionExpression ?? '').trim(),
        triggerActions: supportsActionTriggers(nodeType) ? normalizeActionTriggers(node.triggerActions) : [],
        x: typeof node.x === 'number' ? node.x : 40 + ((index % 6) * 180),
        y: typeof node.y === 'number' ? node.y : 40 + (Math.floor(index / 6) * 120),
    };
};
const normalizeActionTriggers = (values) => {
    if (!Array.isArray(values)) {
        return [];
    }
    const normalized = new Set();
    values.forEach((item) => {
        const upper = String(item ?? '').trim().toUpperCase();
        if (upper === 'CREATE' || upper === 'UPDATE' || upper === 'DELETE') {
            normalized.add(upper);
        }
    });
    return Array.from(normalized);
};
const resetNodeDraft = () => {
    nodeDraft.nodeType = 'NORMAL';
    nodeDraft.nodeName = '';
    nodeDraft.allowUnapprove = true;
    nodeDraft.approverRoleCode = '';
    nodeDraft.roleSignMode = 'OR';
    nodeDraft.approverUserId = undefined;
    nodeDraft.triggerActions = [];
};
const generateNodeKey = () => {
    let cursor = form.nodes.length + 1;
    while (form.nodes.some((item) => item.nodeKey === `node_${cursor}`)) {
        cursor += 1;
    }
    return `node_${cursor}`;
};
const createStartNode = () => normalizeNode({
    nodeType: 'START',
    nodeKey: 'start_node',
    nodeName: '开始',
    x: 88,
    y: 76,
}, 0);
const openAddNodeDialog = () => {
    if (isReadOnlyMode.value) {
        return;
    }
    addAfterNodeKey.value = '';
    resetNodeDraft();
    addDialogVisible.value = true;
};
const syncDraftByType = () => {
    if (nodeDraft.nodeType !== 'SUCCESS') {
        nodeDraft.allowUnapprove = false;
    }
    if (!supportsRoleAssignment(nodeDraft.nodeType)) {
        nodeDraft.approverRoleCode = '';
    }
    if (!supportsApproverUser(nodeDraft.nodeType)) {
        nodeDraft.approverUserId = undefined;
    }
    if (!supportsActionTriggers(nodeDraft.nodeType)) {
        nodeDraft.triggerActions = [];
    }
    if (nodeDraft.nodeType !== 'CONDITION') {
        nodeDraft.roleSignMode = 'OR';
    }
    if (nodeDraft.nodeType === 'SUCCESS' && nodeDraft.allowUnapprove === false) {
        nodeDraft.allowUnapprove = true;
    }
};
const nodeTypeClass = (type) => {
    const key = normalizeNodeTypeValue(type);
    return `node-type-${key.toLowerCase()}`;
};
const getNodeSize = (type) => {
    return { width: 118, height: 38 };
};
const updateCanvasContentSize = () => {
    const paddingX = 160;
    const paddingY = 160;
    const maxRight = form.nodes.reduce((acc, node) => {
        const { width } = getNodeSize(node.nodeType);
        return Math.max(acc, node.x + width);
    }, 0);
    const maxBottom = form.nodes.reduce((acc, node) => {
        const { height } = getNodeSize(node.nodeType);
        return Math.max(acc, node.y + height);
    }, 0);
    canvasContentSize.width = Math.max(1600, canvasSize.width + 320, maxRight + paddingX);
    canvasContentSize.height = Math.max(960, canvasSize.height + 220, maxBottom + paddingY);
};
const MIN_NODE_GAP = 28;
const LAYOUT_PADDING_X = 64;
const LAYOUT_PADDING_Y = 96;
const LAYOUT_GAP_X = 96;
const LAYOUT_GAP_Y = 84;
const rectsOverlap = (a, b, gap = 0) => (a.x - gap < b.x + b.width + gap
    && a.x + a.width + gap > b.x - gap
    && a.y - gap < b.y + b.height + gap
    && a.y + a.height + gap > b.y - gap);
const clampNodePosition = (x, y, type) => {
    const { width, height } = getNodeSize(type);
    return {
        x: Math.max(0, Math.min(x, canvasContentSize.width - width)),
        y: Math.max(0, Math.min(y, canvasContentSize.height - height)),
    };
};
const hasNodeOverlapAt = (nodeKey, x, y, type) => {
    const selfSize = getNodeSize(type);
    const selfRect = { x, y, width: selfSize.width, height: selfSize.height };
    return form.nodes.some((item) => {
        if (item.nodeKey === nodeKey) {
            return false;
        }
        const size = getNodeSize(item.nodeType);
        const rect = { x: item.x, y: item.y, width: size.width, height: size.height };
        return rectsOverlap(selfRect, rect, MIN_NODE_GAP);
    });
};
const findAvailablePosition = (nodeKey, startX, startY, type) => {
    const clamped = clampNodePosition(startX, startY, type);
    if (!hasNodeOverlapAt(nodeKey, clamped.x, clamped.y, type)) {
        return clamped;
    }
    const gap = 24;
    const maxRadius = 20;
    for (let r = 1; r <= maxRadius; r += 1) {
        for (let dx = -r; dx <= r; dx += 1) {
            for (let dy = -r; dy <= r; dy += 1) {
                if (Math.abs(dx) !== r && Math.abs(dy) !== r) {
                    continue;
                }
                const candidate = clampNodePosition(startX + (dx * gap), startY + (dy * gap), type);
                if (!hasNodeOverlapAt(nodeKey, candidate.x, candidate.y, type)) {
                    return candidate;
                }
            }
        }
    }
    return clamped;
};
const normalizeNodePositions = () => {
    const startNodes = form.nodes.filter((node) => node.nodeType === 'START');
    const middleNodes = form.nodes.filter((node) => node.nodeType !== 'START' && node.nodeType !== 'END');
    const endNodes = form.nodes.filter((node) => node.nodeType === 'END');
    const ordered = [...startNodes, ...middleNodes, ...endNodes];
    let cursorX = LAYOUT_PADDING_X;
    let rowY = LAYOUT_PADDING_Y;
    let rowMaxHeight = 0;
    const contentWidth = canvasContentSize.width;
    ordered.forEach((node) => {
        const { width, height } = getNodeSize(node.nodeType);
        if (cursorX + width > contentWidth - 24) {
            cursorX = LAYOUT_PADDING_X;
            rowY += rowMaxHeight + LAYOUT_GAP_Y;
            rowMaxHeight = 0;
        }
        node.x = cursorX;
        node.y = rowY;
        rowMaxHeight = Math.max(rowMaxHeight, height);
        cursorX += width + LAYOUT_GAP_X;
    });
    form.nodes.forEach((node) => {
        const position = findAvailablePosition(node.nodeKey, node.x, node.y, node.nodeType);
        node.x = position.x;
        node.y = position.y;
    });
};
const ensureRoleByNodeType = (node) => {
    const nodeType = normalizeNodeTypeValue(node.nodeType);
    if (!supportsRoleAssignment(nodeType)) {
        node.approverRoleCode = '';
    }
    if (!supportsApproverUser(nodeType)) {
        node.approverUserId = undefined;
    }
    if (!supportsActionTriggers(nodeType)) {
        node.triggerActions = [];
    }
    if (nodeType !== 'CONDITION') {
        node.roleSignMode = 'OR';
    }
    else {
        node.roleSignMode = normalizeRoleSignMode(node.roleSignMode);
    }
    node.allowReject = false;
    if (nodeType !== 'SUCCESS') {
        node.allowUnapprove = false;
    }
};
watch(() => selectedNode.value?.nodeType, () => {
    if (selectedNode.value) {
        ensureRoleByNodeType(selectedNode.value);
    }
});
const closeContextMenu = () => {
    contextMenu.visible = false;
    contextMenu.nodeKey = '';
};
const openNodeContextMenu = (node, event) => {
    if (isReadOnlyMode.value) {
        return;
    }
    event.preventDefault();
    selectedNodeKey.value = node.nodeKey;
    contextMenu.visible = true;
    contextMenu.nodeKey = node.nodeKey;
};
const isFailRollbackTarget = (sourceNodeKey, targetNodeKey) => {
    const sourceNode = form.nodes.find((item) => item.nodeKey === sourceNodeKey);
    const targetNode = form.nodes.find((item) => item.nodeKey === targetNodeKey);
    if (!sourceNode || !targetNode) {
        return false;
    }
    if (normalizeNodeTypeValue(sourceNode.nodeType) !== 'FAIL') {
        return false;
    }
    const targetType = normalizeNodeTypeValue(targetNode.nodeType);
    if (targetType !== 'NORMAL' && targetType !== 'END') {
        return false;
    }
    const sourceIndex = form.nodes.findIndex((item) => item.nodeKey === sourceNodeKey);
    const targetIndex = form.nodes.findIndex((item) => item.nodeKey === targetNodeKey);
    return targetIndex < sourceIndex || targetNode.x < sourceNode.x;
};
const syncEditByType = () => {
    if (editDraft.nodeType !== 'SUCCESS') {
        editDraft.allowUnapprove = false;
    }
    if (!supportsRoleAssignment(editDraft.nodeType)) {
        editDraft.approverRoleCode = '';
    }
    if (!supportsApproverUser(editDraft.nodeType)) {
        editDraft.approverUserId = undefined;
    }
    if (!supportsActionTriggers(editDraft.nodeType)) {
        editDraft.triggerActions = [];
    }
    if (editDraft.nodeType !== 'CONDITION') {
        editDraft.roleSignMode = 'OR';
    }
    if (editDraft.nodeType === 'SUCCESS' && editDraft.allowUnapprove === false) {
        editDraft.allowUnapprove = true;
    }
};
const openEditDialog = (nodeKey) => {
    const node = form.nodes.find((item) => item.nodeKey === nodeKey);
    if (!node) {
        return;
    }
    editDraft.nodeKey = node.nodeKey;
    editDraft.nodeName = node.nodeName;
    editDraft.nodeType = normalizeNodeTypeValue(node.nodeType);
    editDraft.allowUnapprove = Boolean(node.allowUnapprove);
    editDraft.approverRoleCode = String(node.approverRoleCode ?? '').trim();
    editDraft.roleSignMode = normalizeRoleSignMode(node.roleSignMode);
    editDraft.approverUserId = typeof node.approverUserId === 'number' ? node.approverUserId : undefined;
    editDraft.triggerActions = normalizeActionTriggers(node.triggerActions);
    syncEditByType();
    editDialogVisible.value = true;
};
const applyNodeEdit = () => {
    const node = form.nodes.find((item) => item.nodeKey === editDraft.nodeKey);
    if (!node) {
        ElMessage.warning('节点不存在');
        return;
    }
    if (!editDraft.nodeName.trim()) {
        ElMessage.warning('节点名称不能为空');
        return;
    }
    node.nodeName = editDraft.nodeName.trim();
    node.nodeType = editDraft.nodeType;
    node.approverRoleCode = supportsRoleAssignment(editDraft.nodeType) ? editDraft.approverRoleCode : '';
    node.roleSignMode = editDraft.nodeType === 'CONDITION' ? editDraft.roleSignMode : 'OR';
    node.approverUserId = supportsApproverUser(editDraft.nodeType) ? editDraft.approverUserId : undefined;
    node.triggerActions = supportsActionTriggers(editDraft.nodeType) ? normalizeActionTriggers(editDraft.triggerActions) : [];
    node.allowReject = false;
    node.allowUnapprove = editDraft.nodeType === 'SUCCESS' ? editDraft.allowUnapprove : false;
    ensureRoleByNodeType(node);
    editDialogVisible.value = false;
};
const rectRayIntersection = (rx, ry, rw, rh, ox, oy, dx, dy) => {
    const PAD = 3;
    let tMin = Number.NEGATIVE_INFINITY;
    let tMax = Number.POSITIVE_INFINITY;
    if (Math.abs(dx) > 1e-6) {
        let t1 = ((rx + PAD) - ox) / dx;
        let t2 = ((rx + rw - PAD) - ox) / dx;
        if (t1 > t2)
            [t1, t2] = [t2, t1];
        tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
    }
    else {
        return null;
    }
    if (Math.abs(dy) > 1e-6) {
        let t1 = ((ry + PAD) - oy) / dy;
        let t2 = ((ry + rh - PAD) - oy) / dy;
        if (t1 > t2)
            [t1, t2] = [t2, t1];
        tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
    }
    else {
        return null;
    }
    if (tMin > tMax || tMax < 0)
        return null;
    const t = tMin >= 0 ? tMin : tMax;
    return { x: ox + dx * t, y: oy + dy * t };
};
const edgeAnchorPoint = (nodeKey, targetKey, yOffset = 0) => {
    const node = form.nodes.find((item) => item.nodeKey === nodeKey);
    const target = form.nodes.find((item) => item.nodeKey === targetKey);
    if (!node || !target) {
        return { x: 0, y: 0 };
    }
    const size = getNodeSize(node.nodeType);
    const centerX = node.x + (size.width / 2);
    const centerY = node.y + (size.height / 2);
    const targetSize = getNodeSize(target.nodeType);
    const targetCenterX = target.x + (targetSize.width / 2);
    const targetCenterY = target.y + (targetSize.height / 2);
    let dx = targetCenterX - centerX;
    let dy = targetCenterY - centerY;
    const len = Math.hypot(dx, dy);
    if (len < 1e-6) {
        return { x: node.x + size.width, y: centerY };
    }
    dx /= len;
    dy /= len;
    const hit = rectRayIntersection(node.x, node.y, size.width, size.height, centerX + yOffset * 0.01, centerY + yOffset, dx, dy);
    if (hit)
        return hit;
    if (Math.abs(dy) > Math.abs(dx)) {
        if (dy >= 0)
            return { x: centerX, y: node.y + size.height };
        return { x: centerX, y: node.y };
    }
    if (dx >= 0)
        return { x: node.x + size.width, y: centerY };
    return { x: node.x, y: centerY };
};
const edgeSegment = (from, to) => ({
    from: edgeAnchorPoint(from, to),
    to: edgeAnchorPoint(to, from),
});
const pointsEqual = (a, b, epsilon = 0.001) => (Math.abs(a.x - b.x) <= epsilon && Math.abs(a.y - b.y) <= epsilon);
const sharesEndpointOnly = (candidate, current) => (pointsEqual(candidate.from, current.from)
    || pointsEqual(candidate.from, current.to)
    || pointsEqual(candidate.to, current.from)
    || pointsEqual(candidate.to, current.to));
const orientation = (p, q, r) => {
    const value = ((q.y - p.y) * (r.x - q.x)) - ((q.x - p.x) * (r.y - q.y));
    if (Math.abs(value) < 1e-6) {
        return 0;
    }
    return value > 0 ? 1 : 2;
};
const onSegment = (p, q, r) => (q.x <= Math.max(p.x, r.x)
    && q.x >= Math.min(p.x, r.x)
    && q.y <= Math.max(p.y, r.y)
    && q.y >= Math.min(p.y, r.y));
const segmentsIntersect = (a1, a2, b1, b2) => {
    const o1 = orientation(a1, a2, b1);
    const o2 = orientation(a1, a2, b2);
    const o3 = orientation(b1, b2, a1);
    const o4 = orientation(b1, b2, a2);
    if (o1 !== o2 && o3 !== o4) {
        return true;
    }
    if (o1 === 0 && onSegment(a1, b1, a2)) {
        return true;
    }
    if (o2 === 0 && onSegment(a1, b2, a2)) {
        return true;
    }
    if (o3 === 0 && onSegment(b1, a1, b2)) {
        return true;
    }
    if (o4 === 0 && onSegment(b1, a2, b2)) {
        return true;
    }
    return false;
};
const hasEdgeIntersection = (from, to) => {
    const candidate = edgeSegment(from, to);
    return edges.value.some((item) => {
        if (item.from === from && item.to === to) {
            return false;
        }
        const current = edgeSegment(item.from, item.to);
        if (!segmentsIntersect(candidate.from, candidate.to, current.from, current.to)) {
            return false;
        }
        return !sharesEndpointOnly(candidate, current);
    });
};
const appendEdgeIfAbsent = (from, to) => {
    if (from === to) {
        return false;
    }
    const exists = edges.value.some((item) => item.from === from && item.to === to);
    if (exists) {
        return true;
    }
    if (hasEdgeIntersection(from, to)) {
        ElMessage.warning('该连线会与已有连线交叉，请调整节点位置后再连线');
        return false;
    }
    edges.value.push({ id: `${from}__${to}`, from, to });
    return true;
};
const canAppendConditionEdge = (fromNodeKey) => {
    const outgoing = edges.value.filter((item) => item.from === fromNodeKey);
    return outgoing.length < 2;
};
const linkNewNodeAfter = (sourceNodeKey, newNodeKey) => {
    const sourceNode = form.nodes.find((item) => item.nodeKey === sourceNodeKey);
    const sourceNodeType = normalizeNodeTypeValue(sourceNode?.nodeType);
    if (sourceNodeType === 'CONDITION') {
        if (!canAppendConditionEdge(sourceNodeKey)) {
            ElMessage.warning('条件节点最多只能连接两条分支');
            return;
        }
        appendEdgeIfAbsent(sourceNodeKey, newNodeKey);
        return;
    }
    const outgoing = edges.value.find((item) => item.from === sourceNodeKey);
    if (!outgoing) {
        appendEdgeIfAbsent(sourceNodeKey, newNodeKey);
        return;
    }
    const oldTarget = outgoing.to;
    edges.value = edges.value.filter((item) => item.id !== outgoing.id);
    appendEdgeIfAbsent(sourceNodeKey, newNodeKey);
    appendEdgeIfAbsent(newNodeKey, oldTarget);
};
const handleContextAdd = () => {
    if (isReadOnlyMode.value) {
        return;
    }
    if (!contextMenu.nodeKey) {
        return;
    }
    addAfterNodeKey.value = contextMenu.nodeKey;
    resetNodeDraft();
    addDialogVisible.value = true;
    closeContextMenu();
};
const handleContextRollback = (targetNodeKey) => {
    if (isReadOnlyMode.value) {
        return;
    }
    if (!contextMenu.nodeKey) {
        return;
    }
    if (!isFailRollbackTarget(contextMenu.nodeKey, targetNodeKey)) {
        ElMessage.warning('失败节点只能回连到之前的普通节点或结束节点');
        return;
    }
    const existing = edges.value.find((item) => item.from === contextMenu.nodeKey && item.to === targetNodeKey);
    if (existing) {
        closeContextMenu();
        return;
    }
    const oldOutgoing = edges.value.find((item) => item.from === contextMenu.nodeKey);
    if (oldOutgoing) {
        edges.value = edges.value.filter((item) => item.id !== oldOutgoing.id);
    }
    if (hasEdgeIntersection(contextMenu.nodeKey, targetNodeKey)) {
        ElMessage.warning('该连线会与已有连线交叉，请调整节点位置后再连线');
        return;
    }
    edges.value.push({
        id: `${contextMenu.nodeKey}__${targetNodeKey}`,
        from: contextMenu.nodeKey,
        to: targetNodeKey,
        conditionExpression: '',
    });
    closeContextMenu();
};
const addNode = () => {
    const nodeType = nodeDraft.nodeType;
    const nodeKey = generateNodeKey();
    const nodeName = nodeDraft.nodeName.trim();
    if (!nodeName) {
        ElMessage.warning('节点名称不能为空');
        return;
    }
    if (nodeType === 'START' && form.nodes.some((item) => item.nodeType === 'START')) {
        ElMessage.warning('开始节点只能有一个');
        return;
    }
    if (nodeType === 'END' && form.nodes.some((item) => item.nodeType === 'END')) {
        ElMessage.warning('结束节点只能有一个');
        return;
    }
    const node = normalizeNode({
        nodeType,
        nodeKey,
        nodeName,
        allowReject: false,
        allowUnapprove: nodeType === 'SUCCESS' ? nodeDraft.allowUnapprove : false,
        approverRoleCode: supportsRoleAssignment(nodeType) ? nodeDraft.approverRoleCode : '',
        roleSignMode: nodeType === 'CONDITION' ? nodeDraft.roleSignMode : 'OR',
        approverUserId: supportsApproverUser(nodeType) ? nodeDraft.approverUserId : undefined,
        conditionExpression: '',
    }, form.nodes.length);
    if (addAfterNodeKey.value) {
        const source = form.nodes.find((item) => item.nodeKey === addAfterNodeKey.value);
        if (source) {
            const sourceSize = getNodeSize(source.nodeType);
            node.x = source.x + sourceSize.width + LAYOUT_GAP_X;
            node.y = source.y;
            const nodeSize = getNodeSize(node.nodeType);
            node.x = Math.max(0, Math.min(node.x, canvasContentSize.width - nodeSize.width));
            node.y = Math.max(0, Math.min(node.y, canvasContentSize.height - nodeSize.height));
        }
    }
    const availablePosition = findAvailablePosition(node.nodeKey, node.x, node.y, node.nodeType);
    node.x = availablePosition.x;
    node.y = availablePosition.y;
    form.nodes.push(node);
    updateCanvasContentSize();
    if (addAfterNodeKey.value) {
        linkNewNodeAfter(addAfterNodeKey.value, node.nodeKey);
    }
    addAfterNodeKey.value = '';
    selectedNodeKey.value = '';
    addDialogVisible.value = false;
};
const removeNode = (nodeKey) => {
    if (isReadOnlyMode.value) {
        return;
    }
    const node = form.nodes.find((item) => item.nodeKey === nodeKey);
    if (node?.nodeType === 'START') {
        ElMessage.warning('开始节点不允许删除');
        return;
    }
    form.nodes = form.nodes.filter((item) => item.nodeKey !== nodeKey);
    edges.value = edges.value.filter((edge) => edge.from !== nodeKey && edge.to !== nodeKey);
    if (selectedNodeKey.value === nodeKey) {
        selectedNodeKey.value = '';
    }
    if (connectingFromKey.value === nodeKey) {
        connectingFromKey.value = '';
    }
    updateCanvasContentSize();
};
const updateCanvasSize = () => {
    if (!canvasRef.value) {
        return;
    }
    const rect = canvasRef.value.getBoundingClientRect();
    canvasSize.width = Math.max(960, Math.floor(rect.width));
    canvasSize.height = Math.max(560, Math.floor(rect.height));
};
const syncCanvasLayout = () => {
    const previousWidth = lastCanvasWidth;
    updateCanvasSize();
    updateCanvasContentSize();
    if (!canvasRef.value) {
        return;
    }
    if (previousWidth > 0 && canvasSize.width !== previousWidth && !dragState.active) {
        normalizeNodePositions();
    }
    lastCanvasWidth = canvasSize.width;
    updateCanvasContentSize();
};
const beginDrag = (node, event) => {
    if (isReadOnlyMode.value) {
        return;
    }
    if (!canvasRef.value) {
        return;
    }
    const target = event.target;
    if (target.closest('.node-actions')) {
        return;
    }
    selectedNodeKey.value = node.nodeKey;
    const canvasRect = canvasRef.value.getBoundingClientRect();
    dragState.nodeKey = node.nodeKey;
    dragState.active = true;
    dragState.offsetX = event.clientX - canvasRect.left - node.x;
    dragState.offsetY = event.clientY - canvasRect.top - node.y;
};
const onMouseMove = (event) => {
    if (!dragState.active || !canvasRef.value) {
        return;
    }
    const node = form.nodes.find((item) => item.nodeKey === dragState.nodeKey);
    if (!node) {
        return;
    }
    const canvasRect = canvasRef.value.getBoundingClientRect();
    const { width: cardWidth, height: cardHeight } = getNodeSize(node.nodeType);
    const nextX = event.clientX - canvasRect.left - dragState.offsetX;
    const nextY = event.clientY - canvasRect.top - dragState.offsetY;
    const candidateX = Math.max(0, Math.min(nextX, canvasContentSize.width - cardWidth));
    const candidateY = Math.max(0, Math.min(nextY, canvasContentSize.height - cardHeight));
    const position = findAvailablePosition(node.nodeKey, candidateX, candidateY, node.nodeType);
    node.x = position.x;
    node.y = position.y;
    updateCanvasContentSize();
};
const endDrag = () => {
    dragState.active = false;
    dragState.nodeKey = '';
    updateCanvasContentSize();
};
const toggleConnectMode = () => {
    connectingMode.value = !connectingMode.value;
    connectingFromKey.value = '';
};
const connectNodeClick = (node) => {
    if (isReadOnlyMode.value) {
        selectedNodeKey.value = node.nodeKey;
        return;
    }
    if (!connectingMode.value) {
        selectedNodeKey.value = node.nodeKey;
        return;
    }
    if (!connectingFromKey.value) {
        connectingFromKey.value = node.nodeKey;
        ElMessage.info('请选择目标节点完成连线');
        return;
    }
    if (connectingFromKey.value === node.nodeKey) {
        return;
    }
    const sourceNode = form.nodes.find((item) => item.nodeKey === connectingFromKey.value);
    const sourceType = normalizeNodeTypeValue(sourceNode?.nodeType);
    if (sourceType === 'FAIL') {
        if (!isFailRollbackTarget(connectingFromKey.value, node.nodeKey)) {
            ElMessage.warning('失败节点只能回连到之前的普通节点或结束节点');
            connectingFromKey.value = '';
            return;
        }
    }
    const exists = edges.value.some((item) => item.from === connectingFromKey.value && item.to === node.nodeKey);
    if (exists) {
        ElMessage.warning('该连线已存在');
        connectingFromKey.value = '';
        return;
    }
    if (sourceType === 'CONDITION') {
        const outgoing = edges.value.filter((item) => item.from === connectingFromKey.value);
        if (outgoing.length >= 2) {
            ElMessage.warning('条件节点最多只能连接两条分支');
            connectingFromKey.value = '';
            return;
        }
        if (hasEdgeIntersection(connectingFromKey.value, node.nodeKey)) {
            ElMessage.warning('该连线会与已有连线交叉，请调整节点位置后再连线');
            connectingFromKey.value = '';
            return;
        }
        edges.value.push({
            id: `${connectingFromKey.value}__${node.nodeKey}`,
            from: connectingFromKey.value,
            to: node.nodeKey,
            conditionExpression: '',
        });
    }
    else {
        const oldOutgoing = edges.value.find((item) => item.from === connectingFromKey.value);
        if (oldOutgoing) {
            edges.value = edges.value.filter((item) => item.id !== oldOutgoing.id);
        }
        if (hasEdgeIntersection(connectingFromKey.value, node.nodeKey)) {
            ElMessage.warning('该连线会与已有连线交叉，请调整节点位置后再连线');
            connectingFromKey.value = '';
            return;
        }
        edges.value.push({
            id: `${connectingFromKey.value}__${node.nodeKey}`,
            from: connectingFromKey.value,
            to: node.nodeKey,
            conditionExpression: '',
        });
    }
    connectingFromKey.value = '';
};
const nodeCenter = (nodeKey) => {
    const node = form.nodes.find((item) => item.nodeKey === nodeKey);
    if (!node) {
        return { x: 0, y: 0 };
    }
    const { width, height } = getNodeSize(node.nodeType);
    return { x: node.x + (width / 2), y: node.y + (height / 2) };
};
const edgePortOffset = (edge) => {
    const outgoing = edges.value
        .filter((item) => item.from === edge.from)
        .slice()
        .sort((a, b) => {
        const ac = nodeCenter(a.to);
        const bc = nodeCenter(b.to);
        if (ac.y !== bc.y) {
            return ac.y - bc.y;
        }
        return ac.x - bc.x;
    });
    const incoming = edges.value
        .filter((item) => item.to === edge.to)
        .slice()
        .sort((a, b) => {
        const ac = nodeCenter(a.from);
        const bc = nodeCenter(b.from);
        if (ac.y !== bc.y) {
            return ac.y - bc.y;
        }
        return ac.x - bc.x;
    });
    const outIndex = Math.max(0, outgoing.findIndex((item) => item.id === edge.id));
    const inIndex = Math.max(0, incoming.findIndex((item) => item.id === edge.id));
    const outCount = outgoing.length || 1;
    const inCount = incoming.length || 1;
    const startOffset = ((outIndex + 1) / (outCount + 1)) * 2 - 1;
    const endOffset = ((inIndex + 1) / (inCount + 1)) * 2 - 1;
    return { startOffset: startOffset * 10, endOffset: endOffset * 10 };
};
const edgePath = (edge) => {
    const { startOffset, endOffset } = edgePortOffset(edge);
    const from = edgeAnchorPoint(edge.from, edge.to, startOffset);
    const to = edgeAnchorPoint(edge.to, edge.from, endOffset);
    const absDx = Math.abs(to.x - from.x);
    const absDy = Math.abs(to.y - from.y);
    if (absDx >= absDy) {
        const sign = to.x >= from.x ? 1 : -1;
        const arrowGap = 8;
        const endX = to.x - (sign * arrowGap);
        const dist = Math.abs(endX - from.x);
        const dx = Math.max(dist * 0.35, 28);
        const c1x = from.x + (sign * dx);
        const c2x = endX - (sign * dx);
        return `M ${from.x} ${from.y} C ${c1x} ${from.y}, ${c2x} ${to.y}, ${endX} ${to.y}`;
    }
    const signY = to.y >= from.y ? 1 : -1;
    const arrowGapY = 6;
    const endY = to.y - (signY * arrowGapY);
    const distY = Math.abs(endY - from.y);
    const dy = Math.max(distY * 0.35, 22);
    const c1y = from.y + (signY * dy);
    const c2y = endY - (signY * dy);
    return `M ${from.x} ${from.y} C ${from.x} ${c1y}, ${to.x} ${c2y}, ${to.x} ${endY}`;
};
const edgeLabelPoint = (edge) => {
    const { startOffset, endOffset } = edgePortOffset(edge);
    const from = edgeAnchorPoint(edge.from, edge.to, startOffset);
    const to = edgeAnchorPoint(edge.to, edge.from, endOffset);
    const sign = to.x >= from.x ? 1 : -1;
    const textX = Math.abs(to.x - from.x) >= Math.abs(to.y - from.y) ? (to.x - (sign * 8)) : to.x;
    return {
        x: (from.x + textX) / 2,
        y: (from.y + to.y) / 2 - 8,
    };
};
const openEdgeExpression = async (edge) => {
    const sourceNode = form.nodes.find((item) => item.nodeKey === edge.from);
    if (!sourceNode || normalizeNodeTypeValue(sourceNode.nodeType) !== 'CONDITION') {
        return;
    }
    try {
        const { value } = await ElMessageBox.prompt('请输入连线条件表达式', '连线条件', {
            inputValue: edge.conditionExpression || '',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: '例如：amount > 1000',
            closeOnClickModal: false,
        });
        edge.conditionExpression = String(value ?? '').trim();
    }
    catch {
        // canceled
    }
};
const clearEdges = () => {
    edges.value = [];
    updateCanvasContentSize();
};
const toggleBatchUnapproveForSuccessNodes = () => {
    const successNodes = form.nodes.filter((item) => normalizeNodeTypeValue(item.nodeType) === 'SUCCESS');
    if (!successNodes.length) {
        ElMessage.warning('当前没有成功节点可批量反审');
        return;
    }
    const shouldEnable = successNodes.some((item) => !item.allowUnapprove);
    successNodes.forEach((item) => {
        item.allowUnapprove = shouldEnable;
    });
    ElMessage.success(shouldEnable ? '成功节点已批量开启反审核' : '成功节点已批量关闭反审核');
};
const buildEdgesFromNodeConfig = () => {
    edges.value = [];
    let restoredCount = 0;
    form.nodes.forEach((node) => {
        if (!node.conditionExpression) {
            return;
        }
        try {
            const links = JSON.parse(node.conditionExpression);
            if (!Array.isArray(links)) {
                return;
            }
            links.forEach((link) => {
                const targetKey = String(link?.to ?? '').trim();
                if (!targetKey || !form.nodes.some((item) => item.nodeKey === targetKey)) {
                    return;
                }
                const id = `${node.nodeKey}__${targetKey}`;
                if (edges.value.some((item) => item.id === id)) {
                    return;
                }
                edges.value.push({
                    id,
                    from: node.nodeKey,
                    to: targetKey,
                    conditionExpression: String(link?.expression ?? '').trim(),
                });
                restoredCount += 1;
            });
        }
        catch {
            // ignore invalid legacy value
        }
    });
    if (!restoredCount) {
        for (let i = 0; i < form.nodes.length - 1; i += 1) {
            edges.value.push({
                id: `${form.nodes[i].nodeKey}__${form.nodes[i + 1].nodeKey}`,
                from: form.nodes[i].nodeKey,
                to: form.nodes[i + 1].nodeKey,
                conditionExpression: '',
            });
        }
    }
};
const generateWorkflowCode = () => {
    const upperBusiness = (form.businessCode || '').toUpperCase();
    const prefixByBusiness = {
        PURCHASE: 'P',
        ORDER: 'O',
        INVENTORY: 'I',
        PRICE: 'R',
        SALE: 'S',
        FINANCE: 'F',
    };
    const prefixEntry = Object.entries(prefixByBusiness).find(([key]) => upperBusiness.includes(key));
    const prefix = (prefixEntry?.[1] || upperBusiness.replace(/[^A-Z]/g, '').charAt(0) || 'W').toUpperCase();
    const nowPart = Date.now().toString(36).toUpperCase();
    const randPart = Math.random().toString(36).slice(2).toUpperCase();
    const body = `${nowPart}${randPart}`.replace(/[^A-Z0-9]/g, '');
    return `${prefix}${body}`.slice(0, 9).padEnd(9, '0');
};
const formatDateTime = (date) => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
};
const loadApproverOptions = async () => {
    approverLoading.value = true;
    try {
        const [roles, users] = await Promise.all([
            fetchAdminRolesApi(currentOrgId.value),
            fetchAdminUsersApi(),
        ]);
        roleOptions.value = (roles || []).filter((item) => item.status === 'ENABLED');
        userOptions.value = (users || []).filter((item) => item.status === 'ENABLED');
    }
    finally {
        approverLoading.value = false;
    }
};
const loadBusinesses = async () => {
    if (!sessionStore.currentOrgId?.startsWith('group-')) {
        workflowBusinesses.value = [];
        currentBusinessCode.value = '';
        copySourceWorkflowCode.value = '';
        viewWorkflowCode.value = '';
        form.businessCode = '';
        form.workflowName = '';
        form.workflowCode = '';
        return;
    }
    businessLoading.value = true;
    try {
        workflowBusinesses.value = (await fetchWorkflowProcessesApi(sessionStore.currentOrgId))
            .filter((item) => item.processId === TARGET_BUSINESS_CODE);
        if (!workflowBusinesses.value.length) {
            ElMessage.warning(`请先在业务管理中新增“${TARGET_BUSINESS_NAME}（${TARGET_BUSINESS_CODE}）”`);
            currentBusinessCode.value = '';
            copySourceWorkflowCode.value = '';
            viewWorkflowCode.value = '';
            form.businessCode = '';
            form.workflowName = '';
            form.workflowCode = '';
            return;
        }
        const exists = workflowBusinesses.value.some((item) => item.processId === currentBusinessCode.value);
        const targetBusinessCode = exists ? currentBusinessCode.value : workflowBusinesses.value[0].processId;
        applyBusinessSelection(targetBusinessCode);
        if (copySourceWorkflowCode.value) {
            form.workflowCode = '';
            form.deployedAt = '';
        }
    }
    finally {
        businessLoading.value = false;
    }
};
const applyRouteSelection = () => {
    const routeBusinessCode = String(route.query.businessCode ?? '').trim();
    const routeCopyFromWorkflowCode = String(route.query.copyFromWorkflowCode ?? '').trim();
    const routeViewWorkflowCode = String(route.query.viewWorkflowCode ?? '').trim();
    copySourceWorkflowCode.value = routeCopyFromWorkflowCode;
    viewWorkflowCode.value = routeViewWorkflowCode;
    if (routeBusinessCode !== TARGET_BUSINESS_CODE) {
        return;
    }
    const matched = workflowBusinesses.value.find((item) => item.processId === routeBusinessCode);
    if (!matched) {
        return;
    }
    applyBusinessSelection(routeBusinessCode);
    if (routeViewWorkflowCode) {
        form.workflowCode = routeViewWorkflowCode;
        form.deployedAt = '';
        return;
    }
    if (routeCopyFromWorkflowCode) {
        form.workflowCode = '';
        form.deployedAt = '';
    }
};
const onBusinessChange = (value) => {
    applyBusinessSelection(value);
    void loadConfig();
};
const loadConfig = async () => {
    if (!isScopedContext.value) {
        ElMessage.warning('请先切换到集团或门店机构后再配置流程');
        return;
    }
    const sourceWorkflowCode = copySourceWorkflowCode.value.trim();
    const readOnlyWorkflowCode = viewWorkflowCode.value.trim();
    if (!form.businessCode) {
        form.deployedAt = '';
        form.nodes = [createStartNode()];
        selectedNodeKey.value = '';
        buildEdgesFromNodeConfig();
        updateCanvasContentSize();
        return;
    }
    const workflowCodeForLoad = readOnlyWorkflowCode || sourceWorkflowCode || form.workflowCode.trim();
    if (!workflowCodeForLoad) {
        form.deployedAt = '';
        form.nodes = [createStartNode()];
        selectedNodeKey.value = '';
        buildEdgesFromNodeConfig();
        updateCanvasContentSize();
        return;
    }
    loading.value = true;
    try {
        const data = await fetchCurrentWorkflowConfigApi({
            orgId: sessionStore.currentOrgId,
            businessCode: form.businessCode,
            workflowCode: workflowCodeForLoad,
        });
        form.scopeType = data.scopeType ?? '';
        form.workflowName = data.workflowName || form.workflowName;
        if (readOnlyWorkflowCode) {
            form.workflowCode = readOnlyWorkflowCode;
            form.deployedAt = data.deployedAt ?? '';
        }
        else if (sourceWorkflowCode) {
            form.workflowCode = '';
            form.deployedAt = '';
        }
        else {
            form.workflowCode = workflowCodeForLoad;
            form.deployedAt = data.deployedAt ?? '';
        }
        form.nodes = (data.nodes ?? []).map((item, idx) => normalizeNode(item, idx));
        if (!form.nodes.length) {
            form.nodes = [createStartNode()];
        }
        if (!form.nodes.some((item) => item.nodeType === 'START')) {
            form.nodes.unshift(createStartNode());
        }
        const hasPersistedPosition = (data.nodes ?? []).some((item) => typeof item.x === 'number' || typeof item.y === 'number');
        if (!hasPersistedPosition) {
            normalizeNodePositions();
        }
        selectedNodeKey.value = '';
        buildEdgesFromNodeConfig();
        updateCanvasContentSize();
    }
    finally {
        loading.value = false;
    }
};
const saveConfig = async () => {
    if (isReadOnlyMode.value) {
        ElMessage.warning('当前为查看模式，不允许保存');
        return;
    }
    if (!isScopedContext.value) {
        ElMessage.warning('请先切换到集团或门店机构后再保存流程');
        return;
    }
    if (!form.businessCode.trim()) {
        ElMessage.warning('请选择业务');
        return;
    }
    if (!form.nodes.length) {
        ElMessage.warning('请至少新增一个节点');
        return;
    }
    if (!form.nodes.some((item) => item.nodeType === 'START')) {
        ElMessage.warning('缺少开始节点，无法保存');
        return;
    }
    if (!form.nodes.some((item) => item.nodeType === 'END')) {
        ElMessage.warning('缺少结束节点，无法保存');
        return;
    }
    if (!edges.value.length) {
        ElMessage.warning('节点之间缺少连线，无法保存');
        return;
    }
    const incomingCount = new Map();
    const outgoingCount = new Map();
    edges.value.forEach((edge) => {
        outgoingCount.set(edge.from, (outgoingCount.get(edge.from) ?? 0) + 1);
        incomingCount.set(edge.to, (incomingCount.get(edge.to) ?? 0) + 1);
    });
    const unlinkedNode = form.nodes.find((node) => {
        const inCount = incomingCount.get(node.nodeKey) ?? 0;
        const outCount = outgoingCount.get(node.nodeKey) ?? 0;
        if (node.nodeType === 'START') {
            return outCount === 0;
        }
        if (node.nodeType === 'END') {
            return inCount === 0;
        }
        return inCount === 0 || outCount === 0;
    });
    if (unlinkedNode) {
        ElMessage.warning(`节点【${unlinkedNode.nodeName}】未完整连线，无法保存`);
        return;
    }
    const conditionNodes = form.nodes.filter((item) => normalizeNodeTypeValue(item.nodeType) === 'CONDITION');
    if (!conditionNodes.length) {
        ElMessage.warning('至少需要一个条件节点，才可保存草稿');
        return;
    }
    const invalidConditionNode = conditionNodes.find((item) => {
        const hasRole = Boolean(String(item.approverRoleCode ?? '').trim());
        const hasUser = typeof item.approverUserId === 'number' && Number.isFinite(item.approverUserId);
        return !hasRole && !hasUser;
    });
    if (invalidConditionNode) {
        ElMessage.warning(`条件节点【${invalidConditionNode.nodeName}】需选择审批角色或指定人员`);
        return;
    }
    const invalidActionNode = form.nodes.find((item) => {
        if (normalizeNodeTypeValue(item.nodeType) !== 'NORMAL') {
            return false;
        }
        const hasActions = normalizeActionTriggers(item.triggerActions).length > 0;
        const hasRole = Boolean(String(item.approverRoleCode ?? '').trim());
        return hasActions && !hasRole;
    });
    if (invalidActionNode) {
        ElMessage.warning(`普通节点【${invalidActionNode.nodeName}】已选择触发动作，请同时配置审批角色`);
        return;
    }
    saving.value = true;
    try {
        const isCopyMode = Boolean(copySourceWorkflowCode.value.trim());
        if (!form.workflowCode.trim()) {
            form.workflowCode = generateWorkflowCode();
        }
        if (!/^[A-Z][A-Z0-9]{8}$/.test(form.workflowCode)) {
            ElMessage.warning('流程编码必须为9位大写字母数字，且首位为大写字母');
            return;
        }
        await saveCurrentWorkflowConfigApi({
            orgId: sessionStore.currentOrgId,
            businessCode: form.businessCode,
            workflowCode: form.workflowCode,
            workflowName: form.workflowName || form.businessCode,
            nodes: form.nodes.map((node) => ({
                nodeKey: node.nodeKey,
                nodeName: node.nodeName,
                x: Math.round(node.x),
                y: Math.round(node.y),
                approverRoleCode: supportsRoleAssignment(node.nodeType) ? String(node.approverRoleCode ?? '').trim() : '',
                roleSignMode: node.nodeType === 'CONDITION' ? normalizeRoleSignMode(node.roleSignMode) : 'OR',
                approverUserId: supportsApproverUser(node.nodeType) ? node.approverUserId : undefined,
                allowReject: false,
                allowUnapprove: node.nodeType === 'SUCCESS' ? node.allowUnapprove : false,
                nodeType: node.nodeType,
                triggerActions: supportsActionTriggers(node.nodeType) ? normalizeActionTriggers(node.triggerActions) : [],
                conditionExpression: JSON.stringify(edges.value
                    .filter((edge) => edge.from === node.nodeKey)
                    .map((edge) => ({
                    to: edge.to,
                    expression: (edge.conditionExpression || '').trim(),
                }))),
            })),
        });
        if (isCopyMode) {
            copySourceWorkflowCode.value = '';
        }
        if (!form.deployedAt) {
            form.deployedAt = formatDateTime(new Date());
        }
        ElMessage.success('流程版本已保存');
        goBack();
    }
    finally {
        saving.value = false;
    }
};
const goBack = () => {
    if (window.history.length > 1) {
        router.back();
        return;
    }
    router.push('/group/workflow-history');
};
onMounted(async () => {
    await loadApproverOptions();
    await loadBusinesses();
    applyRouteSelection();
    syncCanvasLayout();
    await loadConfig();
    syncCanvasLayout();
    canvasResizeObserver = new ResizeObserver(() => {
        syncCanvasLayout();
    });
    if (canvasRef.value) {
        canvasResizeObserver.observe(canvasRef.value);
    }
    window.addEventListener('resize', syncCanvasLayout);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', endDrag);
    window.addEventListener('click', closeContextMenu);
    window.addEventListener('scroll', closeContextMenu, true);
});
onBeforeUnmount(() => {
    canvasResizeObserver?.disconnect();
    canvasResizeObserver = null;
    window.removeEventListener('resize', syncCanvasLayout);
    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', endDrag);
    window.removeEventListener('click', closeContextMenu);
    window.removeEventListener('scroll', closeContextMenu, true);
});
watch(() => sessionStore.currentOrgId, async () => {
    await loadBusinesses();
    applyRouteSelection();
    void loadConfig();
});
watch(() => [route.query.businessCode, route.query.workflowCode, route.query.copyFromWorkflowCode, route.query.viewWorkflowCode], () => {
    applyRouteSelection();
    void loadConfig();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-delete-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-form']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-form']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-form']} */ ;
/** @type {__VLS_StyleScopedClasses['node-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['floating']} */ ;
/** @type {__VLS_StyleScopedClasses['designer-canvas']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-grid single" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel workflow-panel" },
});
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
/** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
    model: (__VLS_ctx.form),
}));
const __VLS_1 = __VLS_0({
    model: (__VLS_ctx.form),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
const __VLS_3 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
    label: "业务",
}));
const __VLS_5 = __VLS_4({
    label: "业务",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.currentBusinessCode),
    ...{ style: {} },
    loading: (__VLS_ctx.businessLoading),
    filterable: true,
    placeholder: "请选择业务",
}));
const __VLS_9 = __VLS_8({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.currentBusinessCode),
    ...{ style: {} },
    loading: (__VLS_ctx.businessLoading),
    filterable: true,
    placeholder: "请选择业务",
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
let __VLS_11;
let __VLS_12;
let __VLS_13;
const __VLS_14 = {
    onChange: (__VLS_ctx.onBusinessChange)
};
__VLS_10.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.businessOptions))) {
    const __VLS_15 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }));
    const __VLS_17 = __VLS_16({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_16));
}
var __VLS_10;
var __VLS_6;
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "流程编码",
}));
const __VLS_21 = __VLS_20({
    label: "流程编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    modelValue: (__VLS_ctx.form.workflowCode || '-'),
    readonly: true,
    disabled: true,
    ...{ style: {} },
}));
const __VLS_25 = __VLS_24({
    modelValue: (__VLS_ctx.form.workflowCode || '-'),
    readonly: true,
    disabled: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
var __VLS_22;
const __VLS_27 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    label: "发布时间",
}));
const __VLS_29 = __VLS_28({
    label: "发布时间",
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    modelValue: (__VLS_ctx.form.deployedAt || '-'),
    readonly: true,
    disabled: true,
    ...{ style: {} },
}));
const __VLS_33 = __VLS_32({
    modelValue: (__VLS_ctx.form.deployedAt || '-'),
    readonly: true,
    disabled: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
var __VLS_30;
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar compact-toolbar" },
});
const __VLS_35 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    ...{ 'onClick': {} },
}));
const __VLS_37 = __VLS_36({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
let __VLS_39;
let __VLS_40;
let __VLS_41;
const __VLS_42 = {
    onClick: (__VLS_ctx.goBack)
};
__VLS_38.slots.default;
var __VLS_38;
if (!__VLS_ctx.isReadOnlyMode) {
    const __VLS_43 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_44 = __VLS_asFunctionalComponent(__VLS_43, new __VLS_43({
        ...{ 'onClick': {} },
    }));
    const __VLS_45 = __VLS_44({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_44));
    let __VLS_47;
    let __VLS_48;
    let __VLS_49;
    const __VLS_50 = {
        onClick: (__VLS_ctx.toggleBatchUnapproveForSuccessNodes)
    };
    __VLS_46.slots.default;
    var __VLS_46;
    const __VLS_51 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_52 = __VLS_asFunctionalComponent(__VLS_51, new __VLS_51({
        ...{ 'onClick': {} },
        type: (__VLS_ctx.connectingMode ? 'warning' : 'default'),
    }));
    const __VLS_53 = __VLS_52({
        ...{ 'onClick': {} },
        type: (__VLS_ctx.connectingMode ? 'warning' : 'default'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_52));
    let __VLS_55;
    let __VLS_56;
    let __VLS_57;
    const __VLS_58 = {
        onClick: (__VLS_ctx.toggleConnectMode)
    };
    __VLS_54.slots.default;
    (__VLS_ctx.connectingMode ? '退出连线模式' : '连线模式');
    var __VLS_54;
    const __VLS_59 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_60 = __VLS_asFunctionalComponent(__VLS_59, new __VLS_59({
        ...{ 'onClick': {} },
    }));
    const __VLS_61 = __VLS_60({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_60));
    let __VLS_63;
    let __VLS_64;
    let __VLS_65;
    const __VLS_66 = {
        onClick: (__VLS_ctx.clearEdges)
    };
    __VLS_62.slots.default;
    var __VLS_62;
    const __VLS_67 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_68 = __VLS_asFunctionalComponent(__VLS_67, new __VLS_67({
        ...{ 'onClick': {} },
        loading: (__VLS_ctx.saving),
    }));
    const __VLS_69 = __VLS_68({
        ...{ 'onClick': {} },
        loading: (__VLS_ctx.saving),
    }, ...__VLS_functionalComponentArgsRest(__VLS_68));
    let __VLS_71;
    let __VLS_72;
    let __VLS_73;
    const __VLS_74 = {
        onClick: (__VLS_ctx.saveConfig)
    };
    __VLS_70.slots.default;
    var __VLS_70;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.selectedNodeKey = '';
            __VLS_ctx.closeContextMenu();
        } },
    ref: "canvasRef",
    ...{ class: "designer-canvas" },
});
/** @type {typeof __VLS_ctx.canvasRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "designer-canvas__surface" },
    ...{ style: ({ width: `${__VLS_ctx.canvasContentSize.width}px`, height: `${__VLS_ctx.canvasContentSize.height}px` }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    ...{ class: "edge-layer" },
    viewBox: (`0 0 ${__VLS_ctx.canvasContentSize.width} ${__VLS_ctx.canvasContentSize.height}`),
    preserveAspectRatio: "none",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.defs, __VLS_intrinsicElements.defs)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.marker, __VLS_intrinsicElements.marker)({
    id: "arrow",
    markerUnits: "userSpaceOnUse",
    markerWidth: "12",
    markerHeight: "12",
    refX: "12",
    refY: "6",
    orient: "auto",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M0,0 L12,6 L0,12 z",
    fill: "#4f6f95",
});
for (const [edge] of __VLS_getVForSourceType((__VLS_ctx.edges))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        key: (edge.id),
        d: (__VLS_ctx.edgePath(edge)),
        ...{ class: "edge-line" },
        stroke: "#5c7ea7",
        'stroke-width': "2",
        fill: "none",
        'marker-end': "url(#arrow)",
    });
}
for (const [edge] of __VLS_getVForSourceType((__VLS_ctx.edges))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.openEdgeExpression(edge);
            } },
        key: (`${edge.id}_hit`),
        d: (__VLS_ctx.edgePath(edge)),
        ...{ class: "edge-hit" },
        stroke: "transparent",
        'stroke-width': "14",
        fill: "none",
    });
}
for (const [edge] of __VLS_getVForSourceType((__VLS_ctx.edges))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.g, __VLS_intrinsicElements.g)({
        key: (`${edge.id}_label`),
    });
    if (edge.conditionExpression) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.text, __VLS_intrinsicElements.text)({
            x: (__VLS_ctx.edgeLabelPoint(edge).x),
            y: (__VLS_ctx.edgeLabelPoint(edge).y),
            ...{ class: "edge-label" },
        });
        (edge.conditionExpression);
    }
}
for (const [node] of __VLS_getVForSourceType((__VLS_ctx.form.nodes))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onMousedown: (...[$event]) => {
                __VLS_ctx.beginDrag(node, $event);
            } },
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.connectNodeClick(node);
            } },
        ...{ onContextmenu: (...[$event]) => {
                __VLS_ctx.openNodeContextMenu(node, $event);
            } },
        key: (node.nodeKey),
        ...{ class: "node-mini" },
        ...{ class: ({
                active: __VLS_ctx.selectedNodeKey === node.nodeKey,
                connectFrom: __VLS_ctx.connectingFromKey === node.nodeKey,
                [__VLS_ctx.nodeTypeClass(node.nodeType)]: true,
            }) },
        ...{ style: ({ left: `${node.x}px`, top: `${node.y}px` }) },
    });
    if (!__VLS_ctx.isReadOnlyMode && node.nodeType !== 'START') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(!__VLS_ctx.isReadOnlyMode && node.nodeType !== 'START'))
                        return;
                    __VLS_ctx.removeNode(node.nodeKey);
                } },
            ...{ class: "node-delete-btn" },
            type: "button",
            title: "删除节点",
        });
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "node-title" },
    });
    (node.nodeName);
    if (node.nodeType === 'SUCCESS' && node.allowUnapprove) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span)({
            ...{ class: "node-unapprove-tag" },
            title: "可反审",
            'aria-label': "可反审",
        });
    }
    if (!__VLS_ctx.isReadOnlyMode && __VLS_ctx.contextMenu.visible && __VLS_ctx.contextMenu.nodeKey === node.nodeKey) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: () => { } },
            ...{ class: "node-context-menu" },
        });
        if (__VLS_ctx.normalizeNodeTypeValue(node.nodeType) === 'FAIL') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "node-context-menu__divider" },
            });
            for (const [target] of __VLS_getVForSourceType((__VLS_ctx.rollbackTargetNodes))) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!(!__VLS_ctx.isReadOnlyMode && __VLS_ctx.contextMenu.visible && __VLS_ctx.contextMenu.nodeKey === node.nodeKey))
                                return;
                            if (!(__VLS_ctx.normalizeNodeTypeValue(node.nodeType) === 'FAIL'))
                                return;
                            __VLS_ctx.handleContextRollback(target.nodeKey);
                        } },
                    key: (target.nodeKey),
                    type: "button",
                    ...{ class: "node-context-menu__item" },
                });
                (target.nodeName);
            }
            if (!__VLS_ctx.rollbackTargetNodes.length) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "node-context-menu__empty" },
                });
            }
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (__VLS_ctx.handleContextAdd) },
                type: "button",
                ...{ class: "node-context-menu__item" },
            });
        }
    }
}
if (__VLS_ctx.selectedNode) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
        ...{ class: "prop-panel floating" },
    });
    const __VLS_75 = {}.ElForm;
    /** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
    // @ts-ignore
    const __VLS_76 = __VLS_asFunctionalComponent(__VLS_75, new __VLS_75({
        labelWidth: "78px",
        ...{ class: "prop-form" },
    }));
    const __VLS_77 = __VLS_76({
        labelWidth: "78px",
        ...{ class: "prop-form" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_76));
    __VLS_78.slots.default;
    const __VLS_79 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_80 = __VLS_asFunctionalComponent(__VLS_79, new __VLS_79({
        label: "节点编码",
    }));
    const __VLS_81 = __VLS_80({
        label: "节点编码",
    }, ...__VLS_functionalComponentArgsRest(__VLS_80));
    __VLS_82.slots.default;
    const __VLS_83 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_84 = __VLS_asFunctionalComponent(__VLS_83, new __VLS_83({
        modelValue: (__VLS_ctx.selectedNode.nodeKey),
        readonly: true,
    }));
    const __VLS_85 = __VLS_84({
        modelValue: (__VLS_ctx.selectedNode.nodeKey),
        readonly: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_84));
    var __VLS_82;
    const __VLS_87 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_88 = __VLS_asFunctionalComponent(__VLS_87, new __VLS_87({
        label: "节点名称",
    }));
    const __VLS_89 = __VLS_88({
        label: "节点名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_88));
    __VLS_90.slots.default;
    const __VLS_91 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_92 = __VLS_asFunctionalComponent(__VLS_91, new __VLS_91({
        modelValue: (__VLS_ctx.selectedNode.nodeName),
    }));
    const __VLS_93 = __VLS_92({
        modelValue: (__VLS_ctx.selectedNode.nodeName),
    }, ...__VLS_functionalComponentArgsRest(__VLS_92));
    var __VLS_90;
    const __VLS_95 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_96 = __VLS_asFunctionalComponent(__VLS_95, new __VLS_95({
        label: "节点类型",
    }));
    const __VLS_97 = __VLS_96({
        label: "节点类型",
    }, ...__VLS_functionalComponentArgsRest(__VLS_96));
    __VLS_98.slots.default;
    const __VLS_99 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_100 = __VLS_asFunctionalComponent(__VLS_99, new __VLS_99({
        modelValue: (__VLS_ctx.selectedNode.nodeType),
    }));
    const __VLS_101 = __VLS_100({
        modelValue: (__VLS_ctx.selectedNode.nodeType),
    }, ...__VLS_functionalComponentArgsRest(__VLS_100));
    __VLS_102.slots.default;
    const __VLS_103 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({
        label: "普通节点",
        value: "NORMAL",
    }));
    const __VLS_105 = __VLS_104({
        label: "普通节点",
        value: "NORMAL",
    }, ...__VLS_functionalComponentArgsRest(__VLS_104));
    const __VLS_107 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
        label: "条件节点",
        value: "CONDITION",
    }));
    const __VLS_109 = __VLS_108({
        label: "条件节点",
        value: "CONDITION",
    }, ...__VLS_functionalComponentArgsRest(__VLS_108));
    const __VLS_111 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({
        label: "成功节点",
        value: "SUCCESS",
    }));
    const __VLS_113 = __VLS_112({
        label: "成功节点",
        value: "SUCCESS",
    }, ...__VLS_functionalComponentArgsRest(__VLS_112));
    const __VLS_115 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({
        label: "失败节点",
        value: "FAIL",
    }));
    const __VLS_117 = __VLS_116({
        label: "失败节点",
        value: "FAIL",
    }, ...__VLS_functionalComponentArgsRest(__VLS_116));
    const __VLS_119 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({
        label: "开始节点",
        value: "START",
    }));
    const __VLS_121 = __VLS_120({
        label: "开始节点",
        value: "START",
    }, ...__VLS_functionalComponentArgsRest(__VLS_120));
    const __VLS_123 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
        label: "结束节点",
        value: "END",
    }));
    const __VLS_125 = __VLS_124({
        label: "结束节点",
        value: "END",
    }, ...__VLS_functionalComponentArgsRest(__VLS_124));
    var __VLS_102;
    var __VLS_98;
    if (__VLS_ctx.selectedNode.nodeType === 'SUCCESS') {
        const __VLS_127 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
            label: "允许反审核",
        }));
        const __VLS_129 = __VLS_128({
            label: "允许反审核",
        }, ...__VLS_functionalComponentArgsRest(__VLS_128));
        __VLS_130.slots.default;
        const __VLS_131 = {}.ElSwitch;
        /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
        // @ts-ignore
        const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({
            modelValue: (__VLS_ctx.selectedNode.allowUnapprove),
        }));
        const __VLS_133 = __VLS_132({
            modelValue: (__VLS_ctx.selectedNode.allowUnapprove),
        }, ...__VLS_functionalComponentArgsRest(__VLS_132));
        var __VLS_130;
    }
    if (__VLS_ctx.selectedNode.nodeType === 'NORMAL' || __VLS_ctx.selectedNode.nodeType === 'CONDITION') {
        const __VLS_135 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({
            label: "审批角色",
        }));
        const __VLS_137 = __VLS_136({
            label: "审批角色",
        }, ...__VLS_functionalComponentArgsRest(__VLS_136));
        __VLS_138.slots.default;
        const __VLS_139 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
            modelValue: (__VLS_ctx.selectedNode.approverRoleCode),
            clearable: true,
            filterable: true,
            loading: (__VLS_ctx.approverLoading),
            placeholder: "请选择角色",
        }));
        const __VLS_141 = __VLS_140({
            modelValue: (__VLS_ctx.selectedNode.approverRoleCode),
            clearable: true,
            filterable: true,
            loading: (__VLS_ctx.approverLoading),
            placeholder: "请选择角色",
        }, ...__VLS_functionalComponentArgsRest(__VLS_140));
        __VLS_142.slots.default;
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleOptions))) {
            const __VLS_143 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
                key: (item.id),
                label: (`${item.roleName}（${item.roleCode}）`),
                value: (item.roleCode),
            }));
            const __VLS_145 = __VLS_144({
                key: (item.id),
                label: (`${item.roleName}（${item.roleCode}）`),
                value: (item.roleCode),
            }, ...__VLS_functionalComponentArgsRest(__VLS_144));
        }
        var __VLS_142;
        var __VLS_138;
    }
    if (__VLS_ctx.selectedNode.nodeType === 'NORMAL') {
        const __VLS_147 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({
            label: "触发动作",
        }));
        const __VLS_149 = __VLS_148({
            label: "触发动作",
        }, ...__VLS_functionalComponentArgsRest(__VLS_148));
        __VLS_150.slots.default;
        const __VLS_151 = {}.ElCheckboxGroup;
        /** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
        // @ts-ignore
        const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({
            modelValue: (__VLS_ctx.selectedNode.triggerActions),
        }));
        const __VLS_153 = __VLS_152({
            modelValue: (__VLS_ctx.selectedNode.triggerActions),
        }, ...__VLS_functionalComponentArgsRest(__VLS_152));
        __VLS_154.slots.default;
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.ACTION_OPTIONS))) {
            const __VLS_155 = {}.ElCheckbox;
            /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
            // @ts-ignore
            const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
                key: (item.value),
                label: (item.value),
            }));
            const __VLS_157 = __VLS_156({
                key: (item.value),
                label: (item.value),
            }, ...__VLS_functionalComponentArgsRest(__VLS_156));
            __VLS_158.slots.default;
            (item.label);
            var __VLS_158;
        }
        var __VLS_154;
        var __VLS_150;
    }
    if (__VLS_ctx.selectedNode.nodeType === 'CONDITION') {
        const __VLS_159 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({
            label: "会签方式",
        }));
        const __VLS_161 = __VLS_160({
            label: "会签方式",
        }, ...__VLS_functionalComponentArgsRest(__VLS_160));
        __VLS_162.slots.default;
        const __VLS_163 = {}.ElRadioGroup;
        /** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
        // @ts-ignore
        const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({
            modelValue: (__VLS_ctx.selectedNode.roleSignMode),
        }));
        const __VLS_165 = __VLS_164({
            modelValue: (__VLS_ctx.selectedNode.roleSignMode),
        }, ...__VLS_functionalComponentArgsRest(__VLS_164));
        __VLS_166.slots.default;
        const __VLS_167 = {}.ElRadio;
        /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
        // @ts-ignore
        const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({
            label: "OR",
        }));
        const __VLS_169 = __VLS_168({
            label: "OR",
        }, ...__VLS_functionalComponentArgsRest(__VLS_168));
        __VLS_170.slots.default;
        var __VLS_170;
        const __VLS_171 = {}.ElRadio;
        /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
        // @ts-ignore
        const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({
            label: "AND",
        }));
        const __VLS_173 = __VLS_172({
            label: "AND",
        }, ...__VLS_functionalComponentArgsRest(__VLS_172));
        __VLS_174.slots.default;
        var __VLS_174;
        var __VLS_166;
        var __VLS_162;
        const __VLS_175 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
            label: "指定人员",
        }));
        const __VLS_177 = __VLS_176({
            label: "指定人员",
        }, ...__VLS_functionalComponentArgsRest(__VLS_176));
        __VLS_178.slots.default;
        const __VLS_179 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({
            modelValue: (__VLS_ctx.selectedNode.approverUserId),
            clearable: true,
            filterable: true,
            loading: (__VLS_ctx.approverLoading),
            placeholder: "可指定具体审批人",
        }));
        const __VLS_181 = __VLS_180({
            modelValue: (__VLS_ctx.selectedNode.approverUserId),
            clearable: true,
            filterable: true,
            loading: (__VLS_ctx.approverLoading),
            placeholder: "可指定具体审批人",
        }, ...__VLS_functionalComponentArgsRest(__VLS_180));
        __VLS_182.slots.default;
        for (const [user] of __VLS_getVForSourceType((__VLS_ctx.userOptions))) {
            const __VLS_183 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({
                key: (user.id),
                label: (`${user.realName}（${user.phone}）`),
                value: (user.id),
            }));
            const __VLS_185 = __VLS_184({
                key: (user.id),
                label: (`${user.realName}（${user.phone}）`),
                value: (user.id),
            }, ...__VLS_functionalComponentArgsRest(__VLS_184));
        }
        var __VLS_182;
        var __VLS_178;
    }
    var __VLS_78;
}
const __VLS_187 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({
    modelValue: (__VLS_ctx.editDialogVisible),
    title: "修改节点",
    width: "460px",
    destroyOnClose: true,
}));
const __VLS_189 = __VLS_188({
    modelValue: (__VLS_ctx.editDialogVisible),
    title: "修改节点",
    width: "460px",
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_188));
__VLS_190.slots.default;
const __VLS_191 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({
    labelWidth: "90px",
}));
const __VLS_193 = __VLS_192({
    labelWidth: "90px",
}, ...__VLS_functionalComponentArgsRest(__VLS_192));
__VLS_194.slots.default;
const __VLS_195 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_196 = __VLS_asFunctionalComponent(__VLS_195, new __VLS_195({
    label: "节点编码",
}));
const __VLS_197 = __VLS_196({
    label: "节点编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_196));
__VLS_198.slots.default;
const __VLS_199 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
    modelValue: (__VLS_ctx.editDraft.nodeKey),
    readonly: true,
}));
const __VLS_201 = __VLS_200({
    modelValue: (__VLS_ctx.editDraft.nodeKey),
    readonly: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_200));
var __VLS_198;
const __VLS_203 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
    label: "节点名称",
}));
const __VLS_205 = __VLS_204({
    label: "节点名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_204));
__VLS_206.slots.default;
const __VLS_207 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    modelValue: (__VLS_ctx.editDraft.nodeName),
}));
const __VLS_209 = __VLS_208({
    modelValue: (__VLS_ctx.editDraft.nodeName),
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
var __VLS_206;
const __VLS_211 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_212 = __VLS_asFunctionalComponent(__VLS_211, new __VLS_211({
    label: "节点类型",
}));
const __VLS_213 = __VLS_212({
    label: "节点类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_212));
__VLS_214.slots.default;
const __VLS_215 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.editDraft.nodeType),
}));
const __VLS_217 = __VLS_216({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.editDraft.nodeType),
}, ...__VLS_functionalComponentArgsRest(__VLS_216));
let __VLS_219;
let __VLS_220;
let __VLS_221;
const __VLS_222 = {
    onChange: (__VLS_ctx.syncEditByType)
};
__VLS_218.slots.default;
const __VLS_223 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
    label: "普通节点",
    value: "NORMAL",
}));
const __VLS_225 = __VLS_224({
    label: "普通节点",
    value: "NORMAL",
}, ...__VLS_functionalComponentArgsRest(__VLS_224));
const __VLS_227 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
    label: "条件节点",
    value: "CONDITION",
}));
const __VLS_229 = __VLS_228({
    label: "条件节点",
    value: "CONDITION",
}, ...__VLS_functionalComponentArgsRest(__VLS_228));
const __VLS_231 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
    label: "成功节点",
    value: "SUCCESS",
}));
const __VLS_233 = __VLS_232({
    label: "成功节点",
    value: "SUCCESS",
}, ...__VLS_functionalComponentArgsRest(__VLS_232));
const __VLS_235 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
    label: "失败节点",
    value: "FAIL",
}));
const __VLS_237 = __VLS_236({
    label: "失败节点",
    value: "FAIL",
}, ...__VLS_functionalComponentArgsRest(__VLS_236));
const __VLS_239 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    label: "开始节点",
    value: "START",
}));
const __VLS_241 = __VLS_240({
    label: "开始节点",
    value: "START",
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
const __VLS_243 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_244 = __VLS_asFunctionalComponent(__VLS_243, new __VLS_243({
    label: "结束节点",
    value: "END",
}));
const __VLS_245 = __VLS_244({
    label: "结束节点",
    value: "END",
}, ...__VLS_functionalComponentArgsRest(__VLS_244));
var __VLS_218;
var __VLS_214;
if (__VLS_ctx.editDraft.nodeType === 'SUCCESS') {
    const __VLS_247 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
        label: "允许反审核",
    }));
    const __VLS_249 = __VLS_248({
        label: "允许反审核",
    }, ...__VLS_functionalComponentArgsRest(__VLS_248));
    __VLS_250.slots.default;
    const __VLS_251 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_252 = __VLS_asFunctionalComponent(__VLS_251, new __VLS_251({
        modelValue: (__VLS_ctx.editDraft.allowUnapprove),
    }));
    const __VLS_253 = __VLS_252({
        modelValue: (__VLS_ctx.editDraft.allowUnapprove),
    }, ...__VLS_functionalComponentArgsRest(__VLS_252));
    var __VLS_250;
}
if (__VLS_ctx.editDraft.nodeType === 'NORMAL' || __VLS_ctx.editDraft.nodeType === 'CONDITION') {
    const __VLS_255 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
        label: "审批角色",
    }));
    const __VLS_257 = __VLS_256({
        label: "审批角色",
    }, ...__VLS_functionalComponentArgsRest(__VLS_256));
    __VLS_258.slots.default;
    const __VLS_259 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_260 = __VLS_asFunctionalComponent(__VLS_259, new __VLS_259({
        modelValue: (__VLS_ctx.editDraft.approverRoleCode),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "请选择角色",
    }));
    const __VLS_261 = __VLS_260({
        modelValue: (__VLS_ctx.editDraft.approverRoleCode),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "请选择角色",
    }, ...__VLS_functionalComponentArgsRest(__VLS_260));
    __VLS_262.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleOptions))) {
        const __VLS_263 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_264 = __VLS_asFunctionalComponent(__VLS_263, new __VLS_263({
            key: (item.id),
            label: (`${item.roleName}（${item.roleCode}）`),
            value: (item.roleCode),
        }));
        const __VLS_265 = __VLS_264({
            key: (item.id),
            label: (`${item.roleName}（${item.roleCode}）`),
            value: (item.roleCode),
        }, ...__VLS_functionalComponentArgsRest(__VLS_264));
    }
    var __VLS_262;
    var __VLS_258;
}
if (__VLS_ctx.editDraft.nodeType === 'NORMAL') {
    const __VLS_267 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_268 = __VLS_asFunctionalComponent(__VLS_267, new __VLS_267({
        label: "触发动作",
    }));
    const __VLS_269 = __VLS_268({
        label: "触发动作",
    }, ...__VLS_functionalComponentArgsRest(__VLS_268));
    __VLS_270.slots.default;
    const __VLS_271 = {}.ElCheckboxGroup;
    /** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
    // @ts-ignore
    const __VLS_272 = __VLS_asFunctionalComponent(__VLS_271, new __VLS_271({
        modelValue: (__VLS_ctx.editDraft.triggerActions),
    }));
    const __VLS_273 = __VLS_272({
        modelValue: (__VLS_ctx.editDraft.triggerActions),
    }, ...__VLS_functionalComponentArgsRest(__VLS_272));
    __VLS_274.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.ACTION_OPTIONS))) {
        const __VLS_275 = {}.ElCheckbox;
        /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
        // @ts-ignore
        const __VLS_276 = __VLS_asFunctionalComponent(__VLS_275, new __VLS_275({
            key: (item.value),
            label: (item.value),
        }));
        const __VLS_277 = __VLS_276({
            key: (item.value),
            label: (item.value),
        }, ...__VLS_functionalComponentArgsRest(__VLS_276));
        __VLS_278.slots.default;
        (item.label);
        var __VLS_278;
    }
    var __VLS_274;
    var __VLS_270;
}
if (__VLS_ctx.editDraft.nodeType === 'CONDITION') {
    const __VLS_279 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_280 = __VLS_asFunctionalComponent(__VLS_279, new __VLS_279({
        label: "会签方式",
    }));
    const __VLS_281 = __VLS_280({
        label: "会签方式",
    }, ...__VLS_functionalComponentArgsRest(__VLS_280));
    __VLS_282.slots.default;
    const __VLS_283 = {}.ElRadioGroup;
    /** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
    // @ts-ignore
    const __VLS_284 = __VLS_asFunctionalComponent(__VLS_283, new __VLS_283({
        modelValue: (__VLS_ctx.editDraft.roleSignMode),
    }));
    const __VLS_285 = __VLS_284({
        modelValue: (__VLS_ctx.editDraft.roleSignMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_284));
    __VLS_286.slots.default;
    const __VLS_287 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_288 = __VLS_asFunctionalComponent(__VLS_287, new __VLS_287({
        label: "OR",
    }));
    const __VLS_289 = __VLS_288({
        label: "OR",
    }, ...__VLS_functionalComponentArgsRest(__VLS_288));
    __VLS_290.slots.default;
    var __VLS_290;
    const __VLS_291 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_292 = __VLS_asFunctionalComponent(__VLS_291, new __VLS_291({
        label: "AND",
    }));
    const __VLS_293 = __VLS_292({
        label: "AND",
    }, ...__VLS_functionalComponentArgsRest(__VLS_292));
    __VLS_294.slots.default;
    var __VLS_294;
    var __VLS_286;
    var __VLS_282;
    const __VLS_295 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_296 = __VLS_asFunctionalComponent(__VLS_295, new __VLS_295({
        label: "指定人员",
    }));
    const __VLS_297 = __VLS_296({
        label: "指定人员",
    }, ...__VLS_functionalComponentArgsRest(__VLS_296));
    __VLS_298.slots.default;
    const __VLS_299 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_300 = __VLS_asFunctionalComponent(__VLS_299, new __VLS_299({
        modelValue: (__VLS_ctx.editDraft.approverUserId),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "可指定具体审批人",
    }));
    const __VLS_301 = __VLS_300({
        modelValue: (__VLS_ctx.editDraft.approverUserId),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "可指定具体审批人",
    }, ...__VLS_functionalComponentArgsRest(__VLS_300));
    __VLS_302.slots.default;
    for (const [user] of __VLS_getVForSourceType((__VLS_ctx.userOptions))) {
        const __VLS_303 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_304 = __VLS_asFunctionalComponent(__VLS_303, new __VLS_303({
            key: (user.id),
            label: (`${user.realName}（${user.phone}）`),
            value: (user.id),
        }));
        const __VLS_305 = __VLS_304({
            key: (user.id),
            label: (`${user.realName}（${user.phone}）`),
            value: (user.id),
        }, ...__VLS_functionalComponentArgsRest(__VLS_304));
    }
    var __VLS_302;
    var __VLS_298;
}
var __VLS_194;
{
    const { footer: __VLS_thisSlot } = __VLS_190.slots;
    const __VLS_307 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_308 = __VLS_asFunctionalComponent(__VLS_307, new __VLS_307({
        ...{ 'onClick': {} },
    }));
    const __VLS_309 = __VLS_308({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_308));
    let __VLS_311;
    let __VLS_312;
    let __VLS_313;
    const __VLS_314 = {
        onClick: (...[$event]) => {
            __VLS_ctx.editDialogVisible = false;
        }
    };
    __VLS_310.slots.default;
    var __VLS_310;
    const __VLS_315 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_316 = __VLS_asFunctionalComponent(__VLS_315, new __VLS_315({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_317 = __VLS_316({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_316));
    let __VLS_319;
    let __VLS_320;
    let __VLS_321;
    const __VLS_322 = {
        onClick: (__VLS_ctx.applyNodeEdit)
    };
    __VLS_318.slots.default;
    var __VLS_318;
}
var __VLS_190;
const __VLS_323 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_324 = __VLS_asFunctionalComponent(__VLS_323, new __VLS_323({
    modelValue: (__VLS_ctx.addDialogVisible),
    title: "新增节点",
    width: "460px",
    destroyOnClose: true,
}));
const __VLS_325 = __VLS_324({
    modelValue: (__VLS_ctx.addDialogVisible),
    title: "新增节点",
    width: "460px",
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_324));
__VLS_326.slots.default;
const __VLS_327 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_328 = __VLS_asFunctionalComponent(__VLS_327, new __VLS_327({
    labelWidth: "90px",
}));
const __VLS_329 = __VLS_328({
    labelWidth: "90px",
}, ...__VLS_functionalComponentArgsRest(__VLS_328));
__VLS_330.slots.default;
const __VLS_331 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_332 = __VLS_asFunctionalComponent(__VLS_331, new __VLS_331({
    label: "节点类型",
}));
const __VLS_333 = __VLS_332({
    label: "节点类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_332));
__VLS_334.slots.default;
const __VLS_335 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_336 = __VLS_asFunctionalComponent(__VLS_335, new __VLS_335({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.nodeDraft.nodeType),
}));
const __VLS_337 = __VLS_336({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.nodeDraft.nodeType),
}, ...__VLS_functionalComponentArgsRest(__VLS_336));
let __VLS_339;
let __VLS_340;
let __VLS_341;
const __VLS_342 = {
    onChange: (__VLS_ctx.syncDraftByType)
};
__VLS_338.slots.default;
const __VLS_343 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_344 = __VLS_asFunctionalComponent(__VLS_343, new __VLS_343({
    label: "普通节点",
    value: "NORMAL",
}));
const __VLS_345 = __VLS_344({
    label: "普通节点",
    value: "NORMAL",
}, ...__VLS_functionalComponentArgsRest(__VLS_344));
const __VLS_347 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_348 = __VLS_asFunctionalComponent(__VLS_347, new __VLS_347({
    label: "条件节点",
    value: "CONDITION",
}));
const __VLS_349 = __VLS_348({
    label: "条件节点",
    value: "CONDITION",
}, ...__VLS_functionalComponentArgsRest(__VLS_348));
const __VLS_351 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_352 = __VLS_asFunctionalComponent(__VLS_351, new __VLS_351({
    label: "成功节点",
    value: "SUCCESS",
}));
const __VLS_353 = __VLS_352({
    label: "成功节点",
    value: "SUCCESS",
}, ...__VLS_functionalComponentArgsRest(__VLS_352));
const __VLS_355 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_356 = __VLS_asFunctionalComponent(__VLS_355, new __VLS_355({
    label: "失败节点",
    value: "FAIL",
}));
const __VLS_357 = __VLS_356({
    label: "失败节点",
    value: "FAIL",
}, ...__VLS_functionalComponentArgsRest(__VLS_356));
const __VLS_359 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_360 = __VLS_asFunctionalComponent(__VLS_359, new __VLS_359({
    label: "开始节点",
    value: "START",
}));
const __VLS_361 = __VLS_360({
    label: "开始节点",
    value: "START",
}, ...__VLS_functionalComponentArgsRest(__VLS_360));
const __VLS_363 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_364 = __VLS_asFunctionalComponent(__VLS_363, new __VLS_363({
    label: "结束节点",
    value: "END",
}));
const __VLS_365 = __VLS_364({
    label: "结束节点",
    value: "END",
}, ...__VLS_functionalComponentArgsRest(__VLS_364));
var __VLS_338;
var __VLS_334;
const __VLS_367 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_368 = __VLS_asFunctionalComponent(__VLS_367, new __VLS_367({
    label: "节点名称",
}));
const __VLS_369 = __VLS_368({
    label: "节点名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_368));
__VLS_370.slots.default;
const __VLS_371 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_372 = __VLS_asFunctionalComponent(__VLS_371, new __VLS_371({
    modelValue: (__VLS_ctx.nodeDraft.nodeName),
    placeholder: "如：财务审批",
}));
const __VLS_373 = __VLS_372({
    modelValue: (__VLS_ctx.nodeDraft.nodeName),
    placeholder: "如：财务审批",
}, ...__VLS_functionalComponentArgsRest(__VLS_372));
var __VLS_370;
if (__VLS_ctx.nodeDraft.nodeType === 'SUCCESS') {
    const __VLS_375 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_376 = __VLS_asFunctionalComponent(__VLS_375, new __VLS_375({
        label: "允许反审核",
    }));
    const __VLS_377 = __VLS_376({
        label: "允许反审核",
    }, ...__VLS_functionalComponentArgsRest(__VLS_376));
    __VLS_378.slots.default;
    const __VLS_379 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_380 = __VLS_asFunctionalComponent(__VLS_379, new __VLS_379({
        modelValue: (__VLS_ctx.nodeDraft.allowUnapprove),
    }));
    const __VLS_381 = __VLS_380({
        modelValue: (__VLS_ctx.nodeDraft.allowUnapprove),
    }, ...__VLS_functionalComponentArgsRest(__VLS_380));
    var __VLS_378;
}
if (__VLS_ctx.nodeDraft.nodeType === 'NORMAL' || __VLS_ctx.nodeDraft.nodeType === 'CONDITION') {
    const __VLS_383 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_384 = __VLS_asFunctionalComponent(__VLS_383, new __VLS_383({
        label: "审批角色",
    }));
    const __VLS_385 = __VLS_384({
        label: "审批角色",
    }, ...__VLS_functionalComponentArgsRest(__VLS_384));
    __VLS_386.slots.default;
    const __VLS_387 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_388 = __VLS_asFunctionalComponent(__VLS_387, new __VLS_387({
        modelValue: (__VLS_ctx.nodeDraft.approverRoleCode),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "请选择角色",
    }));
    const __VLS_389 = __VLS_388({
        modelValue: (__VLS_ctx.nodeDraft.approverRoleCode),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "请选择角色",
    }, ...__VLS_functionalComponentArgsRest(__VLS_388));
    __VLS_390.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleOptions))) {
        const __VLS_391 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_392 = __VLS_asFunctionalComponent(__VLS_391, new __VLS_391({
            key: (item.id),
            label: (`${item.roleName}（${item.roleCode}）`),
            value: (item.roleCode),
        }));
        const __VLS_393 = __VLS_392({
            key: (item.id),
            label: (`${item.roleName}（${item.roleCode}）`),
            value: (item.roleCode),
        }, ...__VLS_functionalComponentArgsRest(__VLS_392));
    }
    var __VLS_390;
    var __VLS_386;
}
if (__VLS_ctx.nodeDraft.nodeType === 'NORMAL') {
    const __VLS_395 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_396 = __VLS_asFunctionalComponent(__VLS_395, new __VLS_395({
        label: "触发动作",
    }));
    const __VLS_397 = __VLS_396({
        label: "触发动作",
    }, ...__VLS_functionalComponentArgsRest(__VLS_396));
    __VLS_398.slots.default;
    const __VLS_399 = {}.ElCheckboxGroup;
    /** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
    // @ts-ignore
    const __VLS_400 = __VLS_asFunctionalComponent(__VLS_399, new __VLS_399({
        modelValue: (__VLS_ctx.nodeDraft.triggerActions),
    }));
    const __VLS_401 = __VLS_400({
        modelValue: (__VLS_ctx.nodeDraft.triggerActions),
    }, ...__VLS_functionalComponentArgsRest(__VLS_400));
    __VLS_402.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.ACTION_OPTIONS))) {
        const __VLS_403 = {}.ElCheckbox;
        /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
        // @ts-ignore
        const __VLS_404 = __VLS_asFunctionalComponent(__VLS_403, new __VLS_403({
            key: (item.value),
            label: (item.value),
        }));
        const __VLS_405 = __VLS_404({
            key: (item.value),
            label: (item.value),
        }, ...__VLS_functionalComponentArgsRest(__VLS_404));
        __VLS_406.slots.default;
        (item.label);
        var __VLS_406;
    }
    var __VLS_402;
    var __VLS_398;
}
if (__VLS_ctx.nodeDraft.nodeType === 'CONDITION') {
    const __VLS_407 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_408 = __VLS_asFunctionalComponent(__VLS_407, new __VLS_407({
        label: "会签方式",
    }));
    const __VLS_409 = __VLS_408({
        label: "会签方式",
    }, ...__VLS_functionalComponentArgsRest(__VLS_408));
    __VLS_410.slots.default;
    const __VLS_411 = {}.ElRadioGroup;
    /** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
    // @ts-ignore
    const __VLS_412 = __VLS_asFunctionalComponent(__VLS_411, new __VLS_411({
        modelValue: (__VLS_ctx.nodeDraft.roleSignMode),
    }));
    const __VLS_413 = __VLS_412({
        modelValue: (__VLS_ctx.nodeDraft.roleSignMode),
    }, ...__VLS_functionalComponentArgsRest(__VLS_412));
    __VLS_414.slots.default;
    const __VLS_415 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_416 = __VLS_asFunctionalComponent(__VLS_415, new __VLS_415({
        label: "OR",
    }));
    const __VLS_417 = __VLS_416({
        label: "OR",
    }, ...__VLS_functionalComponentArgsRest(__VLS_416));
    __VLS_418.slots.default;
    var __VLS_418;
    const __VLS_419 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_420 = __VLS_asFunctionalComponent(__VLS_419, new __VLS_419({
        label: "AND",
    }));
    const __VLS_421 = __VLS_420({
        label: "AND",
    }, ...__VLS_functionalComponentArgsRest(__VLS_420));
    __VLS_422.slots.default;
    var __VLS_422;
    var __VLS_414;
    var __VLS_410;
    const __VLS_423 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_424 = __VLS_asFunctionalComponent(__VLS_423, new __VLS_423({
        label: "指定人员",
    }));
    const __VLS_425 = __VLS_424({
        label: "指定人员",
    }, ...__VLS_functionalComponentArgsRest(__VLS_424));
    __VLS_426.slots.default;
    const __VLS_427 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_428 = __VLS_asFunctionalComponent(__VLS_427, new __VLS_427({
        modelValue: (__VLS_ctx.nodeDraft.approverUserId),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "可指定具体审批人",
    }));
    const __VLS_429 = __VLS_428({
        modelValue: (__VLS_ctx.nodeDraft.approverUserId),
        clearable: true,
        filterable: true,
        loading: (__VLS_ctx.approverLoading),
        placeholder: "可指定具体审批人",
    }, ...__VLS_functionalComponentArgsRest(__VLS_428));
    __VLS_430.slots.default;
    for (const [user] of __VLS_getVForSourceType((__VLS_ctx.userOptions))) {
        const __VLS_431 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_432 = __VLS_asFunctionalComponent(__VLS_431, new __VLS_431({
            key: (user.id),
            label: (`${user.realName}（${user.phone}）`),
            value: (user.id),
        }));
        const __VLS_433 = __VLS_432({
            key: (user.id),
            label: (`${user.realName}（${user.phone}）`),
            value: (user.id),
        }, ...__VLS_functionalComponentArgsRest(__VLS_432));
    }
    var __VLS_430;
    var __VLS_426;
}
var __VLS_330;
{
    const { footer: __VLS_thisSlot } = __VLS_326.slots;
    const __VLS_435 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_436 = __VLS_asFunctionalComponent(__VLS_435, new __VLS_435({
        ...{ 'onClick': {} },
    }));
    const __VLS_437 = __VLS_436({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_436));
    let __VLS_439;
    let __VLS_440;
    let __VLS_441;
    const __VLS_442 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addDialogVisible = false;
            __VLS_ctx.addAfterNodeKey = '';
        }
    };
    __VLS_438.slots.default;
    var __VLS_438;
    const __VLS_443 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_444 = __VLS_asFunctionalComponent(__VLS_443, new __VLS_443({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_445 = __VLS_444({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_444));
    let __VLS_447;
    let __VLS_448;
    let __VLS_449;
    const __VLS_450 = {
        onClick: (__VLS_ctx.addNode)
    };
    __VLS_446.slots.default;
    var __VLS_446;
}
var __VLS_326;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['workflow-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['designer-canvas']} */ ;
/** @type {__VLS_StyleScopedClasses['designer-canvas__surface']} */ ;
/** @type {__VLS_StyleScopedClasses['edge-layer']} */ ;
/** @type {__VLS_StyleScopedClasses['edge-line']} */ ;
/** @type {__VLS_StyleScopedClasses['edge-hit']} */ ;
/** @type {__VLS_StyleScopedClasses['edge-label']} */ ;
/** @type {__VLS_StyleScopedClasses['node-mini']} */ ;
/** @type {__VLS_StyleScopedClasses['node-delete-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['node-title']} */ ;
/** @type {__VLS_StyleScopedClasses['node-unapprove-tag']} */ ;
/** @type {__VLS_StyleScopedClasses['node-context-menu']} */ ;
/** @type {__VLS_StyleScopedClasses['node-context-menu__divider']} */ ;
/** @type {__VLS_StyleScopedClasses['node-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['node-context-menu__empty']} */ ;
/** @type {__VLS_StyleScopedClasses['node-context-menu__item']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['floating']} */ ;
/** @type {__VLS_StyleScopedClasses['prop-form']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CommonQuerySection: CommonQuerySection,
            loading: loading,
            saving: saving,
            businessLoading: businessLoading,
            approverLoading: approverLoading,
            roleOptions: roleOptions,
            userOptions: userOptions,
            isReadOnlyMode: isReadOnlyMode,
            selectedNodeKey: selectedNodeKey,
            canvasRef: canvasRef,
            canvasContentSize: canvasContentSize,
            connectingMode: connectingMode,
            connectingFromKey: connectingFromKey,
            edges: edges,
            addDialogVisible: addDialogVisible,
            editDialogVisible: editDialogVisible,
            addAfterNodeKey: addAfterNodeKey,
            contextMenu: contextMenu,
            ACTION_OPTIONS: ACTION_OPTIONS,
            normalizeNodeTypeValue: normalizeNodeTypeValue,
            nodeDraft: nodeDraft,
            currentBusinessCode: currentBusinessCode,
            form: form,
            businessOptions: businessOptions,
            selectedNode: selectedNode,
            rollbackTargetNodes: rollbackTargetNodes,
            editDraft: editDraft,
            syncDraftByType: syncDraftByType,
            nodeTypeClass: nodeTypeClass,
            closeContextMenu: closeContextMenu,
            openNodeContextMenu: openNodeContextMenu,
            syncEditByType: syncEditByType,
            applyNodeEdit: applyNodeEdit,
            handleContextAdd: handleContextAdd,
            handleContextRollback: handleContextRollback,
            addNode: addNode,
            removeNode: removeNode,
            beginDrag: beginDrag,
            toggleConnectMode: toggleConnectMode,
            connectNodeClick: connectNodeClick,
            edgePath: edgePath,
            edgeLabelPoint: edgeLabelPoint,
            openEdgeExpression: openEdgeExpression,
            clearEdges: clearEdges,
            toggleBatchUnapproveForSuccessNodes: toggleBatchUnapproveForSuccessNodes,
            onBusinessChange: onBusinessChange,
            saveConfig: saveConfig,
            goBack: goBack,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
