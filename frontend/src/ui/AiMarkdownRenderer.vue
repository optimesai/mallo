<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps<{
  content: string
}>()

marked.setOptions({
  gfm: true,
  breaks: true
})

const sanitizedHtml = computed(() => {
  const raw = marked.parse(props.content, { async: false }) as string
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: [
      'p', 'br', 'hr',
      'strong', 'b', 'em', 'i', 'u', 's', 'del',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li',
      'code', 'pre',
      'blockquote',
      'a',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'span', 'div'
    ],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'class']
  })
})
</script>

<template>
  <div
    class="ai-markdown"
    v-html="sanitizedHtml"
  />
</template>

<style scoped>
.ai-markdown {
  min-width: 0;
  word-break: break-word;
  font-size: 0.8125rem;
  line-height: 1.625;
  color: inherit;
}

.ai-markdown p {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
}

.ai-markdown p:first-child {
  margin-top: 0;
}

.ai-markdown p:last-child {
  margin-bottom: 0;
}

.ai-markdown strong {
  font-weight: 700;
}

.ai-markdown ul {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
  list-style-type: disc;
  padding-left: 1.25rem;
}

.ai-markdown ol {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
  list-style-type: decimal;
  padding-left: 1.25rem;
}

.ai-markdown li {
  margin-top: 0.125rem;
  margin-bottom: 0.125rem;
}

.ai-markdown li > ul,
.ai-markdown li > ol {
  margin-top: 0;
  margin-bottom: 0;
}

.ai-markdown code {
  padding: 0.125rem 0.375rem;
  border-radius: 0.375rem;
  font-size: 0.8125rem;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  background-color: rgba(128, 128, 128, 0.1);
  border: 1px solid rgba(128, 128, 128, 0.15);
}

.ai-markdown pre {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  overflow-x: auto;
  border-radius: 0.5rem;
  padding: 0.75rem;
  background-color: rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(128, 128, 128, 0.15);
}

:global(.dark) .ai-markdown pre {
  background-color: rgba(255, 255, 255, 0.06);
}

.ai-markdown pre code {
  padding: 0;
  border: none;
  background: none;
  font-size: 0.75rem;
  line-height: 1.5;
  color: inherit;
}

.ai-markdown h1,
.ai-markdown h2,
.ai-markdown h3,
.ai-markdown h4 {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.ai-markdown h1 { font-size: 1rem; }
.ai-markdown h2 { font-size: 0.875rem; }
.ai-markdown h3 { font-size: 0.875rem; }

.ai-markdown blockquote {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  border-left: 2px solid;
  padding-left: 0.75rem;
  font-style: italic;
  opacity: 0.8;
  border-color: var(--color-border);
}

.ai-markdown hr {
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
  border: 0;
  border-top: 1px solid var(--color-border-muted);
}

.ai-markdown a {
  text-decoration: underline;
  color: var(--color-primary);
}

.ai-markdown a:hover {
  opacity: 0.8;
}

.ai-markdown table {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
}

.ai-markdown th {
  border: 1px solid;
  border-color: var(--color-border);
  padding: 0.375rem 0.5rem;
  text-align: left;
  font-weight: 600;
  background-color: var(--color-surface-muted);
}

.ai-markdown td {
  border: 1px solid;
  border-color: var(--color-border);
  padding: 0.375rem 0.5rem;
}

.ai-markdown tr:nth-child(even) td {
  background-color: var(--color-surface-muted);
}
</style>
