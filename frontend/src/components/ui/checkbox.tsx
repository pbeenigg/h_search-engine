"use client"

import * as React from "react"
import { Check } from "lucide-react"

import { cn } from "@/lib/utils"

interface CheckboxProps {
  checked?: boolean;
  onCheckedChange?: (checked: boolean) => void;
  disabled?: boolean;
  className?: string;
}

const Checkbox = React.forwardRef<HTMLButtonElement, CheckboxProps>(
  ({ className, checked, onCheckedChange, disabled, ...props }, ref) => {
    return (
      <button
        type="button"
        role="checkbox"
        aria-checked={checked}
        ref={ref}
        className={cn(
          "peer h-4 w-4 shrink-0 rounded-sm border border-gray-300 bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
          checked && "bg-blue-600 border-blue-600 text-white",
          className
        )}
        onClick={() => !disabled && onCheckedChange?.(!checked)}
        disabled={disabled}
        {...props}
      >
        {checked && (
          <Check className="h-3 w-3" />
        )}
      </button>
    )
  }
)
Checkbox.displayName = "Checkbox"

export { Checkbox }