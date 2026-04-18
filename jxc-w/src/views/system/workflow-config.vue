<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import {
  fetchWorkflowProcessesApi,
  fetchCurrentWorkflowConfigApi,
  saveCurrentWorkflowConfigApi,
  type WorkflowProcessItem,
  type WorkflowNode,
} from '@/api/modules/workflow';
import { fetchAdminRolesApi, fetchAdminUsersApi, type RoleAdminItem, type UserAdminItem } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
import { workflowBusinessOptions } from '@/views/inventory/document-meta';

type EditableNode = WorkflowNode & {
  x: number;
  y: number;
};

type EdgeItem = {
  id: string;
  from: string;
  to: string;
  conditionExpression?: string;
};

const sessionStore = useSessionStore();
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const businessLoading = ref(false);
const approverLoading = ref(false);
const workflowBusinesses = ref<WorkflowProcessItem[]>([]);
const roleOptions = ref<RoleAdminItem[]>([]);
const userOptions = ref<UserAdminItem[]>([]);
const currentOrgId = computed(() => sessionStore.currentOrgId || undefined);
const copySourceWorkflowCode = ref('');
const viewWorkflowCode = ref('');
const isReadOnlyMode = computed(() => Boolean(viewWorkflowCode.value));

const selectedNodeKey = ref<string>('');
const canvasRef = ref<HTMLDivElement | null>(null);
const canvasSize = reactive({ width: 1200, height: 560 });
const canvasContentSize = reactive({ width: 1600, height: 960 });
let canvasResizeObserver: ResizeObserver | null = null;
let lastCanvasWidth = 0;

const connectingMode = ref(false);
const connectingFromKey = ref('');
const edges = ref<EdgeItem[]>([]);

const addDialogVisible = ref(false);
const editDialogVisible = ref(false);
const addAfterNodeKey = ref('');
const contextMenu = reactive({
  visible: false,
  nodeKey: '',
});
type NodeType = 'NORMAL' | 'CONDITION' | 'SUCCESS' | 'FAIL' | 'START' | 'END';
type RoleSignMode = 'OR' | 'AND';
type ActionCode = 'CREATE' | 'UPDATE' | 'DELETE';
const ACTION_OPTIONS: Array<{ label: string; value: ActionCode }> = [
  { label: '新增', value: 'CREATE' },
  { label: '修改', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
];
const NODE_TYPE_LABEL: Record<NodeType, string> = {
  NORMAL: '普通节点',
  CONDITION: '条件节点',
  SUCCESS: '成功节点',
  FAIL: '失败节点',
  START: '开始节点',
  END: '结束节点',
};

const normalizeNodeTypeValue = (value?: string): NodeType => {
  if (value === 'CONDITION' || value === 'SUCCESS' || value === 'FAIL' || value === 'START' || value === 'END' || value === 'NORMAL') {
    return value;
  }
  return 'NORMAL';
};

const normalizeRoleSignMode = (value?: string): RoleSignMode => (value === 'AND' ? 'AND' : 'OR');
const supportsRoleAssignment = (type?: string) => {
  const nodeType = normalizeNodeTypeValue(type);
  return nodeType === 'NORMAL' || nodeType === 'CONDITION';
};

const supportsActionTriggers = (type?: string) => normalizeNodeTypeValue(type) === 'NORMAL';
const supportsApproverUser = (type?: string) => normalizeNodeTypeValue(type) === 'CONDITION';

const nodeDraft = reactive({
  nodeType: 'NORMAL' as NodeType,
  nodeName: '',
  allowUnapprove: true,
  approverRoleCode: '',
  roleSignMode: 'OR' as RoleSignMode,
  approverUserId: undefined as number | undefined,
  triggerActions: [] as ActionCode[],
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
  nodes: [] as EditableNode[],
});

const isScopedContext = computed(
  () => sessionStore.currentOrgId.startsWith('group-') || sessionStore.currentOrgId.startsWith('store-'),
);

const businessOptions = computed(() => workflowBusinesses.value.map((item) => ({
  label: item.businessName,
  value: item.processId,
})));

const currentBusiness = computed(
  () => workflowBusinesses.value.find((item) => item.processId === currentBusinessCode.value) ?? null,
);

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
  nodeType: 'NORMAL' as NodeType,
  allowUnapprove: true,
  approverRoleCode: '',
  roleSignMode: 'OR' as RoleSignMode,
  approverUserId: undefined as number | undefined,
  triggerActions: [] as ActionCode[],
});

const applyBusinessSelection = (businessCode: string) => {
  const selected = workflowBusinesses.value.find((item) => item.processId === businessCode) ?? null;
  if (!selected) {
    currentBusinessCode.value = '';
    form.businessCode = '';
    form.workflowName = '';
    form.workflowCode = '';
    form.deployedAt = '';
    return;
  }
  currentBusinessCode.value = businessCode;
  form.businessCode = selected?.processId ?? '';
  form.workflowName = selected?.businessName ?? '';
  form.workflowCode = selected?.templateId?.trim() ?? '';
  form.deployedAt = '';
};

const normalizeNode = (node: Partial<EditableNode>, index: number): EditableNode => {
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

const normalizeActionTriggers = (values?: Array<ActionCode | string>) => {
  if (!Array.isArray(values)) {
    return [] as ActionCode[];
  }
  const normalized = new Set<ActionCode>();
  values.forEach((item) => {
    const upper = String(item ?? '').trim().toUpperCase() as ActionCode;
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

const createStartNode = (): EditableNode => normalizeNode({
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

const nodeTypeClass = (type?: string) => {
  const key = normalizeNodeTypeValue(type);
  return `node-type-${key.toLowerCase()}`;
};

const getNodeSize = (type?: string) => {
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

const rectsOverlap = (
  a: { x: number; y: number; width: number; height: number },
  b: { x: number; y: number; width: number; height: number },
  gap = 0,
) => (
  a.x - gap < b.x + b.width + gap
  && a.x + a.width + gap > b.x - gap
  && a.y - gap < b.y + b.height + gap
  && a.y + a.height + gap > b.y - gap
);

const clampNodePosition = (x: number, y: number, type?: string) => {
  const { width, height } = getNodeSize(type);
  return {
    x: Math.max(0, Math.min(x, canvasContentSize.width - width)),
    y: Math.max(0, Math.min(y, canvasContentSize.height - height)),
  };
};

const hasNodeOverlapAt = (nodeKey: string, x: number, y: number, type?: string) => {
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

const findAvailablePosition = (nodeKey: string, startX: number, startY: number, type?: string) => {
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

const ensureRoleByNodeType = (node: EditableNode) => {
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
  } else {
    node.roleSignMode = normalizeRoleSignMode(node.roleSignMode);
  }
  node.allowReject = false;
  if (nodeType !== 'SUCCESS') {
    node.allowUnapprove = false;
  }
};

watch(
  () => selectedNode.value?.nodeType,
  () => {
    if (selectedNode.value) {
      ensureRoleByNodeType(selectedNode.value);
    }
  },
);

const closeContextMenu = () => {
  contextMenu.visible = false;
  contextMenu.nodeKey = '';
};

const openNodeContextMenu = (node: EditableNode, event: MouseEvent) => {
  if (isReadOnlyMode.value) {
    return;
  }
  event.preventDefault();
  selectedNodeKey.value = node.nodeKey;
  contextMenu.visible = true;
  contextMenu.nodeKey = node.nodeKey;
};

const isFailRollbackTarget = (sourceNodeKey: string, targetNodeKey: string) => {
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

const openEditDialog = (nodeKey: string) => {
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

const rectRayIntersection = (
  rx: number, ry: number, rw: number, rh: number,
  ox: number, oy: number,
  dx: number, dy: number,
) => {
  const PAD = 3;
  let tMin = Number.NEGATIVE_INFINITY;
  let tMax = Number.POSITIVE_INFINITY;

  if (Math.abs(dx) > 1e-6) {
    let t1 = ((rx + PAD) - ox) / dx;
    let t2 = ((rx + rw - PAD) - ox) / dx;
    if (t1 > t2) [t1, t2] = [t2, t1];
    tMin = Math.max(tMin, t1);
    tMax = Math.min(tMax, t2);
  } else {
    return null;
  }

  if (Math.abs(dy) > 1e-6) {
    let t1 = ((ry + PAD) - oy) / dy;
    let t2 = ((ry + rh - PAD) - oy) / dy;
    if (t1 > t2) [t1, t2] = [t2, t1];
    tMin = Math.max(tMin, t1);
    tMax = Math.min(tMax, t2);
  } else {
    return null;
  }

  if (tMin > tMax || tMax < 0) return null;
  const t = tMin >= 0 ? tMin : tMax;
  return { x: ox + dx * t, y: oy + dy * t };
};

const edgeAnchorPoint = (nodeKey: string, targetKey: string, yOffset = 0) => {
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
  dx /= len; dy /= len;

  const hit = rectRayIntersection(
    node.x, node.y, size.width, size.height,
    centerX + yOffset * 0.01, centerY + yOffset,
    dx, dy,
  );
  if (hit) return hit;

  if (Math.abs(dy) > Math.abs(dx)) {
    if (dy >= 0) return { x: centerX, y: node.y + size.height };
    return { x: centerX, y: node.y };
  }
  if (dx >= 0) return { x: node.x + size.width, y: centerY };
  return { x: node.x, y: centerY };
};

const edgeSegment = (from: string, to: string) => ({
  from: edgeAnchorPoint(from, to),
  to: edgeAnchorPoint(to, from),
});

const pointsEqual = (a: { x: number; y: number }, b: { x: number; y: number }, epsilon = 0.001) => (
  Math.abs(a.x - b.x) <= epsilon && Math.abs(a.y - b.y) <= epsilon
);

const sharesEndpointOnly = (
  candidate: { from: { x: number; y: number }; to: { x: number; y: number } },
  current: { from: { x: number; y: number }; to: { x: number; y: number } },
) => (
  pointsEqual(candidate.from, current.from)
  || pointsEqual(candidate.from, current.to)
  || pointsEqual(candidate.to, current.from)
  || pointsEqual(candidate.to, current.to)
);

const orientation = (
  p: { x: number; y: number },
  q: { x: number; y: number },
  r: { x: number; y: number },
) => {
  const value = ((q.y - p.y) * (r.x - q.x)) - ((q.x - p.x) * (r.y - q.y));
  if (Math.abs(value) < 1e-6) {
    return 0;
  }
  return value > 0 ? 1 : 2;
};

const onSegment = (
  p: { x: number; y: number },
  q: { x: number; y: number },
  r: { x: number; y: number },
) => (
  q.x <= Math.max(p.x, r.x)
  && q.x >= Math.min(p.x, r.x)
  && q.y <= Math.max(p.y, r.y)
  && q.y >= Math.min(p.y, r.y)
);

const segmentsIntersect = (
  a1: { x: number; y: number },
  a2: { x: number; y: number },
  b1: { x: number; y: number },
  b2: { x: number; y: number },
) => {
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

const hasEdgeIntersection = (from: string, to: string) => {
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

const appendEdgeIfAbsent = (from: string, to: string) => {
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

const canAppendConditionEdge = (fromNodeKey: string) => {
  const outgoing = edges.value.filter((item) => item.from === fromNodeKey);
  return outgoing.length < 2;
};

const linkNewNodeAfter = (sourceNodeKey: string, newNodeKey: string) => {
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

const handleContextRollback = (targetNodeKey: string) => {
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

  const node: EditableNode = normalizeNode({
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

const removeNode = (nodeKey: string) => {
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

const beginDrag = (node: EditableNode, event: MouseEvent) => {
  if (isReadOnlyMode.value) {
    return;
  }
  if (!canvasRef.value) {
    return;
  }
  const target = event.target as HTMLElement;
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

const onMouseMove = (event: MouseEvent) => {
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

const connectNodeClick = (node: EditableNode) => {
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
  } else {
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

const nodeCenter = (nodeKey: string) => {
  const node = form.nodes.find((item) => item.nodeKey === nodeKey);
  if (!node) {
    return { x: 0, y: 0 };
  }
  const { width, height } = getNodeSize(node.nodeType);
  return { x: node.x + (width / 2), y: node.y + (height / 2) };
};

const edgePortOffset = (edge: EdgeItem) => {
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

const edgePath = (edge: EdgeItem) => {
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

const edgeLabelPoint = (edge: EdgeItem) => {
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

const openEdgeExpression = async (edge: EdgeItem) => {
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
  } catch {
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
      const links = JSON.parse(node.conditionExpression) as Array<{ to?: string; expression?: string }>;
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
    } catch {
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
  const prefixByBusiness: Record<string, string> = {
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

const formatDateTime = (date: Date) => {
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
  } finally {
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
    const businessLabelMap = new Map(workflowBusinessOptions.map((item) => [item.processCode, item.businessName]));
    const optionOrderMap = new Map(workflowBusinessOptions.map((item, index) => [item.processCode, index]));
    workflowBusinesses.value = (await fetchWorkflowProcessesApi(sessionStore.currentOrgId))
      .filter((item) => businessLabelMap.has(item.processId))
      .map((item) => ({
        ...item,
        businessName: businessLabelMap.get(item.processId) ?? item.businessName,
      }))
      .sort((left, right) => (optionOrderMap.get(left.processId) ?? 999) - (optionOrderMap.get(right.processId) ?? 999));
    if (!workflowBusinesses.value.length) {
      ElMessage.warning('请先在业务管理中新增库存审核业务流程');
      currentBusinessCode.value = '';
      copySourceWorkflowCode.value = '';
      viewWorkflowCode.value = '';
      form.businessCode = '';
      form.workflowName = '';
      form.workflowCode = '';
      return;
    }
    const exists = workflowBusinesses.value.some((item) => item.processId === currentBusinessCode.value);
    const routeBusinessCode = String(route.query.businessCode ?? '').trim();
    const targetBusinessCode = exists
      ? currentBusinessCode.value
      : (workflowBusinesses.value.find((item) => item.processId === routeBusinessCode)?.processId ?? workflowBusinesses.value[0].processId);
    applyBusinessSelection(targetBusinessCode);
    if (copySourceWorkflowCode.value) {
      form.workflowCode = '';
      form.deployedAt = '';
    }
  } finally {
    businessLoading.value = false;
  }
};

const applyRouteSelection = () => {
  const routeBusinessCode = String(route.query.businessCode ?? '').trim();
  const routeCopyFromWorkflowCode = String(route.query.copyFromWorkflowCode ?? '').trim();
  const routeViewWorkflowCode = String(route.query.viewWorkflowCode ?? '').trim();
  copySourceWorkflowCode.value = routeCopyFromWorkflowCode;
  viewWorkflowCode.value = routeViewWorkflowCode;
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

const onBusinessChange = (value: string) => {
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
    } else if (sourceWorkflowCode) {
      form.workflowCode = '';
      form.deployedAt = '';
    } else {
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
    const hasPersistedPosition = (data.nodes ?? []).some(
      (item) => typeof item.x === 'number' || typeof item.y === 'number',
    );
    if (!hasPersistedPosition) {
      normalizeNodePositions();
    }
    selectedNodeKey.value = '';
    buildEdgesFromNodeConfig();
    updateCanvasContentSize();
  } finally {
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
  const incomingCount = new Map<string, number>();
  const outgoingCount = new Map<string, number>();
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
        conditionExpression: JSON.stringify(
          edges.value
            .filter((edge) => edge.from === node.nodeKey)
            .map((edge) => ({
              to: edge.to,
              expression: (edge.conditionExpression || '').trim(),
            })),
        ),
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
  } finally {
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

watch(
  () => sessionStore.currentOrgId,
  async () => {
    await loadBusinesses();
    applyRouteSelection();
    void loadConfig();
  },
);

watch(
  () => [route.query.businessCode, route.query.workflowCode, route.query.copyFromWorkflowCode, route.query.viewWorkflowCode],
  () => {
    applyRouteSelection();
    void loadConfig();
  },
);

</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel workflow-panel" v-loading="loading">
      <CommonQuerySection :model="form">
        <el-form-item label="业务">
          <el-select
            v-model="currentBusinessCode"
            style="width: 180px"
            :loading="businessLoading"
            filterable
            placeholder="请选择业务"
            @change="onBusinessChange"
          >
            <el-option v-for="item in businessOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="流程编码">
          <el-input :model-value="form.workflowCode || '-'" readonly disabled style="width: 220px" />
        </el-form-item>
        <el-form-item label="发布时间">
          <el-input :model-value="form.deployedAt || '-'" readonly disabled style="width: 130px" />
        </el-form-item>
      </CommonQuerySection>

      <div class="table-toolbar compact-toolbar">
        <el-button @click="goBack">返回上一页</el-button>
        <template v-if="!isReadOnlyMode">
          <el-button @click="toggleBatchUnapproveForSuccessNodes">批量反审</el-button>
          <el-button :type="connectingMode ? 'warning' : 'default'" @click="toggleConnectMode">
            {{ connectingMode ? '退出连线模式' : '连线模式' }}
          </el-button>
          <el-button @click="clearEdges">清空连线</el-button>
          <el-button :loading="saving" @click="saveConfig">保存版本</el-button>
        </template>
      </div>

      <div ref="canvasRef" class="designer-canvas" @click="selectedNodeKey = ''; closeContextMenu()">
        <div
          class="designer-canvas__surface"
          :style="{ width: `${canvasContentSize.width}px`, height: `${canvasContentSize.height}px` }"
        >
          <svg
            class="edge-layer"
            :viewBox="`0 0 ${canvasContentSize.width} ${canvasContentSize.height}`"
            preserveAspectRatio="none"
          >
            <defs>
              <marker id="arrow" markerUnits="userSpaceOnUse" markerWidth="12" markerHeight="12" refX="12" refY="6" orient="auto">
                <path d="M0,0 L12,6 L0,12 z" fill="#4f6f95" />
              </marker>
            </defs>
            <path
              v-for="edge in edges"
              :key="edge.id"
              :d="edgePath(edge)"
              class="edge-line"
              stroke="#5c7ea7"
              stroke-width="2"
              fill="none"
              marker-end="url(#arrow)"
            />
            <path
              v-for="edge in edges"
              :key="`${edge.id}_hit`"
              :d="edgePath(edge)"
              class="edge-hit"
              stroke="transparent"
              stroke-width="14"
              fill="none"
              @click.stop="openEdgeExpression(edge)"
            />
            <g v-for="edge in edges" :key="`${edge.id}_label`">
              <text
                v-if="edge.conditionExpression"
                :x="edgeLabelPoint(edge).x"
                :y="edgeLabelPoint(edge).y"
                class="edge-label"
              >
                {{ edge.conditionExpression }}
              </text>
            </g>
          </svg>

          <div
            v-for="node in form.nodes"
            :key="node.nodeKey"
            class="node-mini"
            :class="{
              active: selectedNodeKey === node.nodeKey,
              connectFrom: connectingFromKey === node.nodeKey,
              [nodeTypeClass(node.nodeType)]: true,
            }"
            :style="{ left: `${node.x}px`, top: `${node.y}px` }"
            @mousedown.stop="beginDrag(node, $event)"
            @click.stop="connectNodeClick(node)"
            @contextmenu.prevent.stop="openNodeContextMenu(node, $event)"
          >
            <button
              v-if="!isReadOnlyMode && node.nodeType !== 'START'"
              class="node-delete-btn"
              type="button"
              title="删除节点"
              @click.stop="removeNode(node.nodeKey)"
            >
              ×
            </button>
            <div class="node-title">{{ node.nodeName }}</div>
            <span
              v-if="node.nodeType === 'SUCCESS' && node.allowUnapprove"
              class="node-unapprove-tag"
              title="可反审"
              aria-label="可反审"
            />
            <div
              v-if="!isReadOnlyMode && contextMenu.visible && contextMenu.nodeKey === node.nodeKey"
              class="node-context-menu"
              @click.stop
            >
              <template v-if="normalizeNodeTypeValue(node.nodeType) === 'FAIL'">
                <div class="node-context-menu__divider">选择节点</div>
                <button
                  v-for="target in rollbackTargetNodes"
                  :key="target.nodeKey"
                  type="button"
                  class="node-context-menu__item"
                  @click="handleContextRollback(target.nodeKey)"
                >
                  {{ target.nodeName }}
                </button>
                <div v-if="!rollbackTargetNodes.length" class="node-context-menu__empty">暂无可选节点</div>
              </template>
              <button
                v-else
                type="button"
                class="node-context-menu__item"
                @click="handleContextAdd"
              >
                添加节点
              </button>
            </div>
          </div>
        </div>
      </div>

      <aside class="prop-panel floating" v-if="selectedNode">
        <el-form label-width="78px" class="prop-form">
          <el-form-item label="节点编码">
            <el-input v-model="selectedNode.nodeKey" readonly />
          </el-form-item>
          <el-form-item label="节点名称">
            <el-input v-model="selectedNode.nodeName" />
          </el-form-item>
          <el-form-item label="节点类型">
            <el-select v-model="selectedNode.nodeType">
              <el-option label="普通节点" value="NORMAL" />
              <el-option label="条件节点" value="CONDITION" />
              <el-option label="成功节点" value="SUCCESS" />
              <el-option label="失败节点" value="FAIL" />
              <el-option label="开始节点" value="START" />
              <el-option label="结束节点" value="END" />
            </el-select>
          </el-form-item>
          <template v-if="selectedNode.nodeType === 'SUCCESS'">
            <el-form-item label="允许反审核">
              <el-switch v-model="selectedNode.allowUnapprove" />
            </el-form-item>
          </template>
          <template v-if="selectedNode.nodeType === 'NORMAL' || selectedNode.nodeType === 'CONDITION'">
            <el-form-item label="审批角色">
              <el-select
                v-model="selectedNode.approverRoleCode"
                clearable
                filterable
                :loading="approverLoading"
                placeholder="请选择角色"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.id"
                  :label="`${item.roleName}（${item.roleCode}）`"
                  :value="item.roleCode"
                />
              </el-select>
            </el-form-item>
          </template>
          <template v-if="selectedNode.nodeType === 'NORMAL'">
            <el-form-item label="触发动作">
              <el-checkbox-group v-model="selectedNode.triggerActions">
                <el-checkbox v-for="item in ACTION_OPTIONS" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </template>
          <template v-if="selectedNode.nodeType === 'CONDITION'">
            <el-form-item label="会签方式">
              <el-radio-group v-model="selectedNode.roleSignMode">
                <el-radio label="OR">或签</el-radio>
                <el-radio label="AND">会签</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="指定人员">
              <el-select
                v-model="selectedNode.approverUserId"
                clearable
                filterable
                :loading="approverLoading"
                placeholder="可指定具体审批人"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="`${user.realName}（${user.phone}）`"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </template>
        </el-form>
      </aside>

      <el-dialog v-model="editDialogVisible" title="修改节点" width="460px" destroy-on-close>
        <el-form label-width="90px">
          <el-form-item label="节点编码">
            <el-input v-model="editDraft.nodeKey" readonly />
          </el-form-item>
          <el-form-item label="节点名称">
            <el-input v-model="editDraft.nodeName" />
          </el-form-item>
          <el-form-item label="节点类型">
            <el-select v-model="editDraft.nodeType" @change="syncEditByType">
              <el-option label="普通节点" value="NORMAL" />
              <el-option label="条件节点" value="CONDITION" />
              <el-option label="成功节点" value="SUCCESS" />
              <el-option label="失败节点" value="FAIL" />
              <el-option label="开始节点" value="START" />
              <el-option label="结束节点" value="END" />
            </el-select>
          </el-form-item>
          <template v-if="editDraft.nodeType === 'SUCCESS'">
            <el-form-item label="允许反审核">
              <el-switch v-model="editDraft.allowUnapprove" />
            </el-form-item>
          </template>
          <template v-if="editDraft.nodeType === 'NORMAL' || editDraft.nodeType === 'CONDITION'">
            <el-form-item label="审批角色">
              <el-select
                v-model="editDraft.approverRoleCode"
                clearable
                filterable
                :loading="approverLoading"
                placeholder="请选择角色"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.id"
                  :label="`${item.roleName}（${item.roleCode}）`"
                  :value="item.roleCode"
                />
              </el-select>
            </el-form-item>
          </template>
          <template v-if="editDraft.nodeType === 'NORMAL'">
            <el-form-item label="触发动作">
              <el-checkbox-group v-model="editDraft.triggerActions">
                <el-checkbox v-for="item in ACTION_OPTIONS" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </template>
          <template v-if="editDraft.nodeType === 'CONDITION'">
            <el-form-item label="会签方式">
              <el-radio-group v-model="editDraft.roleSignMode">
                <el-radio label="OR">或签</el-radio>
                <el-radio label="AND">会签</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="指定人员">
              <el-select
                v-model="editDraft.approverUserId"
                clearable
                filterable
                :loading="approverLoading"
                placeholder="可指定具体审批人"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="`${user.realName}（${user.phone}）`"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="applyNodeEdit">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="addDialogVisible" title="新增节点" width="460px" destroy-on-close>
        <el-form label-width="90px">
          <el-form-item label="节点类型">
            <el-select v-model="nodeDraft.nodeType" @change="syncDraftByType">
              <el-option label="普通节点" value="NORMAL" />
              <el-option label="条件节点" value="CONDITION" />
              <el-option label="成功节点" value="SUCCESS" />
              <el-option label="失败节点" value="FAIL" />
              <el-option label="开始节点" value="START" />
              <el-option label="结束节点" value="END" />
            </el-select>
          </el-form-item>
          <el-form-item label="节点名称">
            <el-input v-model="nodeDraft.nodeName" placeholder="如：财务审批" />
          </el-form-item>
          <template v-if="nodeDraft.nodeType === 'SUCCESS'">
            <el-form-item label="允许反审核">
              <el-switch v-model="nodeDraft.allowUnapprove" />
            </el-form-item>
          </template>
          <template v-if="nodeDraft.nodeType === 'NORMAL' || nodeDraft.nodeType === 'CONDITION'">
            <el-form-item label="审批角色">
              <el-select
                v-model="nodeDraft.approverRoleCode"
                clearable
                filterable
                :loading="approverLoading"
                placeholder="请选择角色"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.id"
                  :label="`${item.roleName}（${item.roleCode}）`"
                  :value="item.roleCode"
                />
              </el-select>
            </el-form-item>
          </template>
          <template v-if="nodeDraft.nodeType === 'NORMAL'">
            <el-form-item label="触发动作">
              <el-checkbox-group v-model="nodeDraft.triggerActions">
                <el-checkbox v-for="item in ACTION_OPTIONS" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </template>
          <template v-if="nodeDraft.nodeType === 'CONDITION'">
            <el-form-item label="会签方式">
              <el-radio-group v-model="nodeDraft.roleSignMode">
                <el-radio label="OR">或签</el-radio>
                <el-radio label="AND">会签</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="指定人员">
              <el-select
                v-model="nodeDraft.approverUserId"
                clearable
                filterable
                :loading="approverLoading"
                placeholder="可指定具体审批人"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="`${user.realName}（${user.phone}）`"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="addDialogVisible = false; addAfterNodeKey = ''">取消</el-button>
          <el-button type="primary" @click="addNode">确定</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<style scoped>
.workflow-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 92px);
  min-height: 0;
  overflow: hidden;
}

.workflow-form-compact {
  margin-bottom: 6px;
}

.compact-toolbar {
  margin-bottom: 8px;
}

.designer-canvas {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  border: 1px dashed #c6d2e1;
  border-radius: 8px;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f7fd 100%);
  overflow-x: auto;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.designer-canvas__surface {
  position: relative;
}

.edge-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: auto;
}

.edge-line {
  pointer-events: none;
}

.edge-hit {
  cursor: pointer;
  pointer-events: stroke;
}

.edge-label {
  fill: #334155;
  font-size: 11px;
  font-weight: 600;
  text-anchor: middle;
  paint-order: stroke;
  stroke: #f8fbff;
  stroke-width: 3px;
  stroke-linejoin: round;
}

.node-mini {
  position: absolute;
  width: 118px;
  min-height: 38px;
  padding: 6px 8px;
  border: 1px solid #d7e2ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(31, 53, 85, 0.08);
  cursor: move;
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.node-mini.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
}

.node-mini.connectFrom {
  border-color: #e6a23c;
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.18);
}

.node-mini.node-type-start {
  background: #ecfdf3;
  border-color: #95dbaf;
}

.node-mini.node-type-end {
  background: #fff1f2;
  border-color: #f3b2bc;
}

.node-mini.node-type-normal {
  background: #f8fbff;
  border-color: #b9d3f3;
}

.node-mini.node-type-condition {
  background: #f5f3ff;
  border-color: #c8b5ff;
}

.node-mini.node-type-success {
  background: #ecfdf5;
  border-color: #86efac;
}

.node-mini.node-type-fail {
  background: #fef2f2;
  border-color: #fca5a5;
}

.node-title {
  font-size: 11px;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 14px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-unapprove-tag {
  position: absolute;
  left: 6px;
  top: 6px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  border: 1px solid #b9e3c8;
  background: #22c55e;
  box-shadow: 0 0 0 1px #ffffff;
  z-index: 1;
}

.node-delete-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 14px;
  height: 14px;
  border: 1px solid #d3ddeb;
  border-radius: 50%;
  background: #fff;
  color: #8a99ad;
  font-size: 10px;
  line-height: 12px;
  text-align: center;
  cursor: pointer;
  padding: 0;
}

.node-delete-btn:hover {
  color: #f56c6c;
  border-color: #f7b1b1;
}

.prop-panel {
  border: 1px solid #d8e2ef;
  border-radius: 10px;
  background: #fff;
  padding: 8px 10px;
  box-shadow: 0 2px 12px rgba(21, 44, 75, 0.08);
  width: 248px;
}

.prop-panel.floating {
  position: fixed;
  left: 18px;
  bottom: 18px;
  top: auto;
  right: auto;
  max-height: calc(100vh - 36px);
  overflow: auto;
  z-index: 30;
}

.prop-form :deep(.el-form-item) {
  margin-bottom: 6px;
}

.prop-form :deep(.el-form-item__label) {
  font-size: 12px;
  line-height: 20px;
}

.prop-form :deep(.el-input__wrapper),
.prop-form :deep(.el-select__wrapper) {
  min-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}

.node-context-menu {
  position: absolute;
  z-index: 2500;
  left: calc(100% + 6px);
  top: 0;
  min-width: 116px;
  border: 1px solid #d8e2ef;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(17, 24, 39, 0.14);
  padding: 4px 0;
}

.node-context-menu__item {
  display: block;
  width: 100%;
  border: 0;
  background: transparent;
  text-align: left;
  padding: 6px 10px;
  font-size: 11px;
  line-height: 1.2;
  color: #334155;
  cursor: pointer;
}

.node-context-menu__item:hover {
  background: #eef4ff;
  color: #1f6feb;
}

.node-context-menu__divider {
  font-size: 10px;
  color: #64748b;
  padding: 4px 10px 2px;
  border-top: 1px solid #eef2f7;
  margin-top: 2px;
}

.node-context-menu__empty {
  font-size: 10px;
  color: #94a3b8;
  padding: 6px 10px;
}

@media (max-width: 1200px) {
  .prop-panel.floating {
    position: static;
    width: 100%;
    max-height: none;
    margin-top: 8px;
  }

  .designer-canvas {
    min-height: 420px;
  }
}
</style>
