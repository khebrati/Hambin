# Project purpose

This repository is a design prototype and design-system reference.

It is not the production application.

## Primary objective

Explore, validate, and document the complete product experience using an interactive browser-based prototype informed by Material 3 Expressive.

## General rules

- Invoke the material-3-expressive skill for major design tasks.
- Do not create backend services, databases, authentication infrastructure, or production application architecture.
- Use local mock data.
- Optimize for fast visual iteration.
- Prefer semantic design tokens over raw visual values.
- Keep components reusable but avoid premature abstraction.
- Use realistic content instead of lorem ipsum.
- Treat accessibility as part of the design, not a later implementation task.
- Support light theme, dark theme, large text, RTL, and responsive layouts.
- Do not add a component library that visually conflicts with Material 3.
- Do not imitate existing products too closely.

## Prototype technology

- Use React, TypeScript, and Vite.
- Use CSS variables for design tokens.
- Use lightweight dependencies.
- Avoid production state-management or networking libraries.
- Implement interactions locally.
- Use mobile-first responsive layouts.

## Design process

For substantial design work:

1. Inspect existing documentation and components.
2. State assumptions.
3. Present the proposed design direction.
4. Make a focused implementation.
5. Run the prototype.
6. Review visual and interaction results.
7. Document accepted decisions.

## Completion criteria

A screen is not complete unless its relevant states are represented:

- Initial
- Populated
- Loading
- Empty
- Error
- Disabled
- Selected
- Pressed or active
- Offline, where relevant