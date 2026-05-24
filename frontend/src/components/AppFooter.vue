<script setup lang="ts">
import { Mail } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const currentYear = computed(() => new Date().getFullYear())
</script>

<template>
  <footer class="app-footer">
    <div class="footer-shell">
      <div class="footer-top">
        <RouterLink class="footer-brand" :to="{ name: 'home' }">
          <span class="brand-mark" aria-hidden="true">g</span>
          <span class="brand-text">
            <span class="brand-name">Activities Club</span>
            <span class="brand-tagline">sport · adventure · wellness ✿</span>
          </span>
        </RouterLink>

        <nav class="footer-links" aria-label="Footer">
          <RouterLink class="footer-link" :to="{ name: 'home' }">Home</RouterLink>
          <RouterLink class="footer-link" :to="{ name: 'activities' }">Activities</RouterLink>
          <RouterLink class="footer-link" :to="{ name: 'sports-club-signup' }">
            Sports Club Signup
          </RouterLink>
        </nav>

        <a class="footer-contact" href="mailto:activities@griffith.ie">
          <Mail class="h-4 w-4" aria-hidden="true" />
          <span>activities@griffith.ie</span>
        </a>
      </div>

      <div class="footer-coda">
        <p class="footer-copy">&copy; {{ currentYear }} Activities Club</p>
        <p class="footer-note">made with grit &amp; granola in Dublin</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  position: relative;
  margin-top: 4rem;
  padding: 3.5rem 1rem 1.75rem;
  background: var(--primary);
  color: var(--primary-foreground);
  isolation: isolate;

  --wave-tile: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 14' width='80' height='14'><path d='M0 14 L0 7 Q10 1, 20 7 T40 7 T60 7 T80 7 L80 14 Z' fill='black'/></svg>");

  -webkit-mask-image: var(--wave-tile), linear-gradient(black, black);
  -webkit-mask-size: 80px 14px, 100% calc(100% - 14px);
  -webkit-mask-position: top left, bottom left;
  -webkit-mask-repeat: repeat-x, no-repeat;
  mask-image: var(--wave-tile), linear-gradient(black, black);
  mask-size: 80px 14px, 100% calc(100% - 14px);
  mask-position: top left, bottom left;
  mask-repeat: repeat-x, no-repeat;
}

.footer-shell {
  position: relative;
  width: 100%;
  max-width: 80rem;
  margin: 0 auto;
  padding: 0 1.25rem;
}
@media (min-width: 640px) {
  .app-footer {
    padding-left: 1.5rem;
    padding-right: 1.5rem;
  }
  .footer-shell {
    padding-left: 1.5rem;
    padding-right: 1.5rem;
  }
}
@media (min-width: 1024px) {
  .app-footer {
    padding-left: 2rem;
    padding-right: 2rem;
  }
  .footer-shell {
    padding-left: 2rem;
    padding-right: 2rem;
  }
}

.footer-top {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2rem;
  align-items: center;
}
@media (min-width: 900px) {
  .footer-top {
    grid-template-columns: auto 1fr auto;
    gap: 2.5rem;
  }
}

/* Brand */
.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  text-decoration: none;
  color: var(--primary-foreground);
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-ochre);
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.02em;
  padding-top: 2px;
  box-shadow:
    3px 3px 0 var(--color-coral),
    6px 6px 0 #0b1b3d;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.footer-brand:hover .brand-mark {
  transform: translate(-2px, -2px);
  box-shadow:
    5px 5px 0 var(--color-coral),
    8px 8px 0 #0b1b3d;
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1;
}
.brand-name {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 26px;
  letter-spacing: -0.005em;
  color: var(--primary-foreground);
}
.brand-tagline {
  margin-top: 6px;
  font-family: var(--font-hand);
  font-weight: 500;
  font-size: 18px;
  line-height: 1;
  color: var(--color-ochre);
}

/* Links */
.footer-links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 0.5rem 0.75rem;
}
@media (min-width: 900px) {
  .footer-links {
    justify-content: center;
  }
}
.footer-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--primary-foreground);
  text-decoration: none;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 17px;
  letter-spacing: 0.01em;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    transform 0.15s ease;
}
.footer-link:hover {
  background: var(--color-ochre);
  color: var(--primary);
  transform: translate(-1px, -1px);
}

/* Contact pill */
.footer-contact {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 22px;
  background: var(--color-coral);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  border-radius: 999px;
  text-decoration: none;
  box-shadow:
    3px 3px 0 var(--color-ochre),
    6px 6px 0 #0b1b3d;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.footer-contact:hover {
  transform: translate(-2px, -2px);
  box-shadow:
    5px 5px 0 var(--color-ochre),
    8px 8px 0 #0b1b3d;
}

/* Coda */
.footer-coda {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 2.5rem;
  padding-top: 1.25rem;
  border-top: 1px dashed color-mix(in srgb, var(--primary-foreground) 30%, transparent);
  font-size: 13px;
  color: color-mix(in srgb, var(--primary-foreground) 75%, transparent);
  text-align: center;
}
.footer-copy,
.footer-note {
  margin: 0;
}
.footer-note {
  font-family: var(--font-hand);
  font-size: 16px;
  color: var(--color-ochre);
}
@media (min-width: 640px) {
  .footer-coda {
    flex-direction: row;
    justify-content: space-between;
    text-align: left;
  }
}

@media (max-width: 899px) {
  .footer-contact {
    align-self: flex-start;
  }
}
</style>
