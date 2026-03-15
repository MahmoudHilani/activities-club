import { cva } from 'class-variance-authority'

export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-all duration-200 outline-none focus-visible:ring-4 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default:
          'bg-primary text-primary-foreground shadow-[0_14px_32px_rgba(38,70,173,0.24)] hover:-translate-y-0.5 hover:brightness-105',
        secondary:
          'bg-secondary text-secondary-foreground hover:bg-[color-mix(in_srgb,var(--secondary)_84%,white)]',
        outline:
          'border border-border bg-white/75 text-foreground hover:bg-white',
        ghost:
          'text-muted-foreground hover:bg-white/70 hover:text-foreground',
        destructive:
          'bg-destructive text-destructive-foreground hover:brightness-105',
      },
      size: {
        default: 'h-11 px-5',
        sm: 'h-9 px-4 text-sm',
        lg: 'h-12 px-6 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)
