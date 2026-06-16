<script setup lang="ts">
import type { BomTreeNode } from '@/api/bomMasterApi'

const props = defineProps<{
  nodes: BomTreeNode[]
  relationKey: 'children' | 'parents'
  emptyText: string
  showCumulative?: boolean
  baseQuantity?: number
  parentRequiredQuantity?: number
}>()

function relationNodes(node: BomTreeNode) {
  return props.relationKey === 'children' ? node.children || [] : node.parents || []
}

function requiredQuantity(node: BomTreeNode) {
  const parentQuantity = props.parentRequiredQuantity ?? props.baseQuantity ?? 1
  if (!node.bomVersion) return props.baseQuantity ?? 1
  return parentQuantity * Number(node.quantity || 0)
}

function formatQuantity(value: number) {
  return Math.trunc(Number(value) || 0).toLocaleString()
}
</script>

<template>
  <div v-if="nodes.length === 0" class="bom-tree-empty">
    {{ emptyText }}
  </div>
  <ul v-else class="bom-tree-list">
    <li v-for="node in nodes" :key="`${node.itemId}-${node.bomVersion || 'root'}`" class="bom-tree-item">
      <div class="bom-tree-node">
        <div class="bom-tree-main">
          <span class="bom-tree-code">{{ node.itemCode }}</span>
          <span class="bom-tree-name">{{ node.itemName }}</span>
        </div>
        <div class="bom-tree-meta-group">
          <span class="bom-tree-badge">{{ node.itemType }}</span>
          <span class="bom-tree-meta">소요량 {{ formatQuantity(node.quantity) }} {{ node.unit }}</span>
          <span v-if="showCumulative" class="bom-tree-required">누적 {{ formatQuantity(requiredQuantity(node)) }} {{ node.unit }}</span>
          <span v-if="node.bomVersion" class="bom-tree-version">{{ node.bomVersion }}</span>
        </div>
      </div>
      <BomTreeList
        v-if="relationNodes(node).length > 0"
        :nodes="relationNodes(node)"
        :relation-key="relationKey"
        :empty-text="emptyText"
        :show-cumulative="showCumulative"
        :base-quantity="baseQuantity"
        :parent-required-quantity="requiredQuantity(node)"
      />
    </li>
  </ul>
</template>

<style scoped>
.bom-tree-empty {
  padding: 2.5rem 1rem;
  color: var(--color-text-muted);
  text-align: center;
  font-size: 0.875rem;
  font-weight: var(--font-weight-label);
}

.bom-tree-list {
  display: grid;
  gap: 0.625rem;
  padding-left: 0;
  list-style: none;
}

.bom-tree-list .bom-tree-list {
  margin-top: 0.625rem;
  margin-left: 0.875rem;
  padding-left: 0.875rem;
  border-left: 1px dashed var(--color-border-strong);
}

.bom-tree-item {
  min-width: 0;
}

.bom-tree-node {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border-muted);
  border-radius: 1rem;
}

.bom-tree-main,
.bom-tree-meta-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.bom-tree-code {
  color: var(--color-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 0.75rem;
  font-weight: var(--font-weight-strong);
}

.bom-tree-name {
  color: var(--color-text-strong);
  font-size: 0.875rem;
  font-weight: var(--font-weight-label);
}

.bom-tree-badge,
.bom-tree-version,
.bom-tree-required {
  padding: 0.2rem 0.5rem;
  background: var(--color-surface-muted);
  border-radius: var(--radius-pill);
  color: var(--color-text-soft);
  font-size: 0.75rem;
  font-weight: var(--font-weight-label);
}

.bom-tree-meta {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: var(--font-weight-label);
}

.bom-tree-required {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.bom-tree-version {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

@media (max-width: 640px) {
  .bom-tree-node {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
