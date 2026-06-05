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
  return Number(value || 0).toFixed(2)
}
</script>

<template>
  <div v-if="nodes.length === 0" class="bom-tree-empty">
    {{ emptyText }}
  </div>
  <ul v-else class="bom-tree-list">
    <li v-for="node in nodes" :key="`${node.itemId}-${node.bomVersion || 'root'}`" class="bom-tree-item">
      <div class="bom-tree-node">
        <span class="bom-tree-code">{{ node.itemCode }}</span>
        <span class="bom-tree-name">{{ node.itemName }}</span>
        <span class="bom-tree-badge">{{ node.itemType }}</span>
        <span class="bom-tree-meta">소요량 {{ formatQuantity(node.quantity) }} {{ node.unit }}</span>
        <span v-if="showCumulative" class="bom-tree-required">누적 {{ formatQuantity(requiredQuantity(node)) }} {{ node.unit }}</span>
        <span v-if="node.bomVersion" class="bom-tree-version">{{ node.bomVersion }}</span>
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
  padding: 3rem 1rem;
  color: var(--bom-color-text-muted);
  text-align: center;
  font-size: var(--bom-font-size-sm);
}

.bom-tree-list {
  display: grid;
  gap: 0.75rem;
  padding-left: 0;
  list-style: none;
}

.bom-tree-list .bom-tree-list {
  margin-top: 0.75rem;
  margin-left: 1.25rem;
  padding-left: 1rem;
  border-left: 1px dashed var(--bom-color-border-strong);
}

.bom-tree-item {
  min-width: 0;
}

.bom-tree-node {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 0.875rem;
  background: var(--bom-color-surface);
  border: 1px solid var(--bom-color-border);
  border-radius: var(--bom-radius-control);
}

.bom-tree-code {
  color: var(--bom-color-primary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-strong);
}

.bom-tree-name {
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-sm);
  font-weight: var(--bom-font-weight-label);
}

.bom-tree-badge,
.bom-tree-version,
.bom-tree-required {
  padding: 0.125rem 0.5rem;
  background: var(--bom-color-surface-muted);
  border-radius: var(--bom-radius-pill);
  color: var(--bom-color-text-secondary);
  font-size: var(--bom-font-size-2xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-tree-meta {
  color: var(--bom-color-text-muted);
  font-size: var(--bom-font-size-xs);
}

.bom-tree-required {
  background: var(--bom-color-primary-soft);
  color: var(--bom-color-primary);
}
</style>
