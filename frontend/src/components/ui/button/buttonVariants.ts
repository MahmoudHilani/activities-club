import { cva } from 'class-variance-authority'

export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full font-bold tracking-tight leading-none transition-all duration-150 outline-none focus-visible:ring-4 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default:
          'bg-primary text-primary-foreground shadow-[3px_3px_0_var(--color-coral),6px_6px_0_var(--color-ochre)] hover:-translate-x-[2px] hover:-translate-y-[2px] hover:shadow-[5px_5px_0_var(--color-coral),8px_8px_0_var(--color-ochre)]',
        secondary:
          'border-2 border-primary bg-[color-mix(in_srgb,white_78%,#f4efe4_22%)] text-primary shadow-[3px_3px_0_var(--primary)] hover:-translate-x-[2px] hover:-translate-y-[2px] hover:shadow-[5px_5px_0_var(--primary)]',
        outline:
          'border-2 border-primary bg-white text-primary shadow-[3px_3px_0_var(--primary)] hover:-translate-x-[2px] hover:-translate-y-[2px] hover:shadow-[5px_5px_0_var(--color-coral)]',
        ghost:
          'text-primary hover:bg-[color-mix(in_srgb,var(--primary)_8%,transparent)]',
        destructive:
          'bg-[var(--color-coral)] text-white shadow-[3px_3px_0_var(--primary)] hover:-translate-x-[2px] hover:-translate-y-[2px] hover:shadow-[5px_5px_0_var(--primary)]',
      },
      size: {
        default: 'h-11 px-5 text-sm',
        sm: 'h-9 px-4 text-xs uppercase tracking-[0.05em]',
        lg: 'h-[3.25rem] px-7 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)
