# Rendering decision

Use one Kotlin View and a cached four-cubic Path with a rotating SweepGradient. The original diagonal midpoints and eight control points are retained up to uniform scaling and translation. The cubic extrema factor is 0.875 + 0.125 × tan(shapeAngle); fitting those extrema avoids clipping.

ValueAnimator follows display timing, with cancellation on visibility/window/detach and the system animation-scale setting. Keep a requested loading flag separate from whether animation can currently run. A transparent canvas removes the need for background masks. Stop freezes the current phase; start resumes it.

A Compose rewrite is unnecessary for this small renderer. AndroidView provides integration without forcing Compose dependencies on consumers. Keep the project to library and sample modules; no extra architecture or benchmark module.
