import { createApp } from 'vue'
import { VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import { pinia } from './lib/pinia'
import { queryClient } from './lib/query-client'
import router from './router'
import { useSessionStore } from './stores/session'
import './style.css'

const app = createApp(App)

app.use(pinia)
app.use(VueQueryPlugin, { queryClient })

const sessionStore = useSessionStore(pinia)
await sessionStore.hydrate()

app.use(router)

app.mount('#app')
