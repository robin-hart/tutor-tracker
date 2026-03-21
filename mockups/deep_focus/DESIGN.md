# Design System Document

## 1. Overview & Creative North Star: "The Orchestrated Flow"

This design system is not a kit of parts; it is a framework for cognitive clarity. In the high-stakes environment of productivity and time-tracking, users don't need more "tools"—they need an environment that recedes to let their work breathe.

**The Creative North Star: The Orchestrated Flow.**
We break the "SaaS Template" look by moving away from rigid, boxed grids and heavy borders. Instead, we utilize **Editorial Asymmetry** and **Tonal Depth**. By using wide margins (Spacing 20-24) and intentional overlapping of elements, we create a sense of bespoke craftsmanship. The interface should feel like a high-end physical planner: premium, authoritative, and tactically silent.

---

## 2. Colors: Depth Over Definition

The palette centers on a sophisticated interplay of slate neutrals and a deep, intellectual Indigo (`primary`). 

### The "No-Line" Rule
**Explicit Instruction:** Prohibit the use of 1px solid borders for sectioning or layout containment. 
Boundaries must be defined solely through background color shifts. For example, a sidebar using `surface_container_low` should sit directly against a `surface` main content area. The eye perceives the edge through the change in tone, creating a "limitless" feel that reduces visual noise.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked, semi-transparent sheets. 
*   **Base Layer:** `surface` (#f8f9ff)
*   **Secondary Context:** `surface_container_low` (#eff4ff)
*   **Actionable Containers:** `surface_container_highest` (#d3e4fe)

### The "Glass & Gradient" Rule
To elevate the experience from "Standard UI" to "Premium Tool," use Glassmorphism for floating panels (e.g., time-tracking popovers). Apply `surface_container_lowest` at 80% opacity with a `backdrop-blur` of 20px. 
*   **Signature Texture:** Main CTAs should not be flat. Apply a subtle linear gradient from `primary` (#24389c) to `primary_container` (#3f51b5) at a 135-degree angle to provide "soul" and a sense of pressable weight.

---

## 3. Typography: Editorial Authority

We pair the precision of **Inter** with the architectural character of **Manrope**.

*   **Display & Headlines (Manrope):** Used for data summaries and page titles. The wide apertures of Manrope convey a modern, open feel. Use `display-lg` (3.5rem) for daily time totals to give the data an "editorial" importance.
*   **Body & Labels (Inter):** Used for all functional text. Inter’s high x-height ensures readability in complex calendar views.
*   **Visual Hierarchy Tip:** Never use pure black. All text must use `on_surface` (#0b1c30) or `on_surface_variant` (#454652) to maintain a soft, professional contrast that reduces eye strain during long tracking sessions.

---

## 4. Elevation & Depth: Tonal Layering

Traditional drop shadows are banned in favor of **Ambient Light** and **Tonal Lift**.

*   **The Layering Principle:** Achieve depth by stacking tiers. Place a `surface_container_lowest` (#ffffff) card on a `surface_container_low` (#eff4ff) background. The 8-12px `DEFAULT` corner radius creates a soft, pillowy lift that feels natural.
*   **Ambient Shadows:** For high-priority floating elements (e.g., a "Start Timer" modal), use a highly diffused shadow: `box-shadow: 0 20px 40px rgba(11, 28, 48, 0.06)`. The shadow color is a tinted version of `on_surface`, not grey.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility in data tables, use `outline_variant` (#c5c5d4) at **15% opacity**. It should be felt, not seen.

---

## 5. Components: Functional Elegance

### Buttons & Chips
*   **Primary Action:** Gradient-filled (Primary to Primary Container), `DEFAULT` (0.5rem) radius.
*   **Secondary/Ghost:** No border. Use `secondary_container` background with `on_secondary_container` text.
*   **Chips:** Use `secondary_fixed` (#d5e3fc) for category tags. Forbid borders; use 0.4rem (`spacing-2`) horizontal padding to keep them tight and professional.

### Inputs & Fields
*   **State Management:** Use `surface_container_highest` for the input track. Upon focus, do not change the border; instead, shift the background to `surface_bright` and apply a 2px "glow" using `primary` at 20% opacity.

### The "Calendar & Data" Pattern
*   **Gridless Lists:** Forbid the use of divider lines between tasks. Instead, use `spacing-3` (0.6rem) of vertical white space.
*   **Time-Blocks:** Use `primary_fixed` (#dee0ff) for scheduled blocks. The low-contrast background allows the `on_primary_fixed_variant` text to remain the focal point.

### Custom Component: The "Focus Shroud"
A full-screen overlay using `surface_dim` at 60% opacity with a heavy blur, used when a user enters "Deep Work" mode, isolating a single task card in the center of the screen.

---

## 6. Do's and Don'ts

### Do
*   **DO** use `spacing-16` and `spacing-20` for page margins to create a high-end, editorial "airy" feel.
*   **DO** use `tertiary` (#6c3400) sparingly for "Warning" or "Overtime" states to provide a sophisticated alternative to standard "Error Red."
*   **DO** use `surface_container_lowest` for the most important interactive cards to make them "pop" against the tinted background.

### Don't
*   **DON'T** use 1px solid borders (except for the 15% opacity "Ghost Border" fallback).
*   **DON'T** use pure grey shadows; always tint shadows with the `on_surface` blue-slate.
*   **DON'T** crowd data. If a calendar view feels tight, increase the container size rather than decreasing the font size. Maintain the hierarchy of the Typography Scale at all costs.