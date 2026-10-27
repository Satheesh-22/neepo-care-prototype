#!/usr/bin/env python3
"""
prepare-report-images.py

Transforms raw full-resolution Playwright screenshots into professional,
document-ready figures for technical project reports, academic papers, and Word/PDF/Markdown docs.

Operations performed:
1. High-quality 2x downsampling (Lanczos filter) to standardized widths (1440px / 1200px)
2. Precise semantic cropping of tall pages (e.g., PostGIS corridor map, explainability matrices)
3. Side-by-side mobile responsive comparison composites
4. Clean 1px subtle framing border (#CBD5E1) to prevent bleed against white document pages
5. Dual output format: High-clarity PNG and lightweight WebP
"""

import os
import sys
from PIL import Image, ImageOps

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIR = os.path.join(BASE_DIR, "qa", "screenshots")
OUT_DIR = os.path.join(BASE_DIR, "docs", "images")

os.makedirs(OUT_DIR, exist_ok=True)

def add_border(img, color=(203, 213, 225), width=1):
    """Add a subtle perimeter border around the image."""
    return ImageOps.expand(img, border=width, fill=color)

def save_dual(img, base_name):
    """Save both PNG and WebP versions."""
    png_path = os.path.join(OUT_DIR, f"{base_name}.png")
    webp_path = os.path.join(OUT_DIR, f"{base_name}.webp")
    
    img.save(png_path, "PNG", optimize=True)
    img.save(webp_path, "WEBP", quality=92, method=6)
    
    png_kb = os.path.getsize(png_path) / 1024
    webp_kb = os.path.getsize(webp_path) / 1024
    print(f"  -> {base_name}: {img.size[0]}x{img.size[1]} | PNG: {png_kb:.1f} KB | WebP: {webp_kb:.1f} KB")

def main():
    print("Preparing project report figures in:", OUT_DIR)
    
    # -------------------------------------------------------------
    # Fig 1: Login Portal & Role Switcher
    # -------------------------------------------------------------
    print("\n[1/8] Processing Fig 1: Login Portal...")
    p1 = os.path.join(SRC_DIR, "01_login_portal.png")
    if os.path.exists(p1):
        img1 = Image.open(p1)
        # 2880x1800 -> 1440x900
        fig1 = img1.resize((1440, 900), Image.Resampling.LANCZOS)
        fig1 = add_border(fig1)
        save_dual(fig1, "fig01_login_portal")

    # -------------------------------------------------------------
    # Fig 2: Operations Dashboard Overview
    # -------------------------------------------------------------
    print("\n[2/8] Processing Fig 2: Operations Dashboard...")
    p2 = os.path.join(SRC_DIR, "02_operations_dashboard.png")
    if os.path.exists(p2):
        img2 = Image.open(p2)
        # 2880x2208 -> 1440x1104
        fig2 = img2.resize((1440, 1104), Image.Resampling.LANCZOS)
        fig2 = add_border(fig2)
        save_dual(fig2, "fig02_operations_dashboard")

    # -------------------------------------------------------------
    # Fig 3: CARE Incident Corridor & Route Map (Cropped top)
    # -------------------------------------------------------------
    print("\n[3/8] Processing Fig 3: Incident Corridor Map...")
    p3 = os.path.join(SRC_DIR, "03_care_decision_workspace.png")
    if os.path.exists(p3):
        img3 = Image.open(p3)
        # Crop top navbar, header, Leaflet map, and lifecycle tracker: y=0 to 1725
        crop3 = img3.crop((0, 0, 2880, 1725))
        fig3_h = int(1725 * (1440 / 2880))
        fig3 = crop3.resize((1440, fig3_h), Image.Resampling.LANCZOS)
        fig3 = add_border(fig3)
        save_dual(fig3, "fig03_care_incident_corridor_map")

        # -------------------------------------------------------------
        # Fig 4: Hard Constraint Evaluation & Candidate Rejection
        # -------------------------------------------------------------
        print("\n[4/8] Processing Fig 4: Candidate Alternatives & Constraint Filtering...")
        # Crop alternatives header and first candidate cards + explainability overview: y=1740 to 4190
        crop4 = img3.crop((0, 1740, 2880, 4190))
        fig4_h = int((4190 - 1740) * (1440 / 2880))
        fig4 = crop4.resize((1440, fig4_h), Image.Resampling.LANCZOS)
        fig4 = add_border(fig4)
        save_dual(fig4, "fig04_care_candidate_evaluations")

        # -------------------------------------------------------------
        # Fig 5: Optimal Decision Selection & Mathematical Explainability
        # -------------------------------------------------------------
        print("\n[5/8] Processing Fig 5: Decision Selection & Explainability Panel...")
        # Left card: Emergency Escalation card (passed hard constraints)
        card_selected = img3.crop((40, 8660, 1725, 9360))
        # Right card: Why CARE Selected This (Utility equation, deterministic tie-break)
        card_explain = img3.crop((1770, 1835, 2840, 2835))
        
        # Combine side-by-side on clean canvas
        pad = 24
        spacing = 30
        w_comb = card_selected.size[0] + card_explain.size[0] + spacing + (pad * 2)
        h_comb = max(card_selected.size[1], card_explain.size[1]) + (pad * 2)
        
        canvas = Image.new("RGB", (w_comb, h_comb), color=(248, 250, 252))
        # Vertical center alignment
        y_sel = pad + (h_comb - (pad * 2) - card_selected.size[1]) // 2
        y_exp = pad + (h_comb - (pad * 2) - card_explain.size[1]) // 2
        
        canvas.paste(card_selected, (pad, y_sel))
        canvas.paste(card_explain, (pad + card_selected.size[0] + spacing, y_exp))
        
        # Scale to standard 1440 width
        fig5_h = int(canvas.size[1] * (1440 / canvas.size[0]))
        fig5 = canvas.resize((1440, fig5_h), Image.Resampling.LANCZOS)
        fig5 = add_border(fig5)
        save_dual(fig5, "fig05_care_optimal_decision_selection")

        # -------------------------------------------------------------
        # Fig 8: Complete Full-Page Decision Workspace (Scaled overview)
        # -------------------------------------------------------------
        print("\n[6/8] Processing Fig 8: Full Decision Workspace Overview...")
        fig8_h = int(9406 * (1200 / 2880))
        fig8 = img3.resize((1200, fig8_h), Image.Resampling.LANCZOS)
        fig8 = add_border(fig8)
        save_dual(fig8, "fig08_full_decision_workspace")

    # -------------------------------------------------------------
    # Fig 6: Telemetry Simulator & Sensor Injection
    # -------------------------------------------------------------
    print("\n[7/8] Processing Fig 6: Telemetry Simulator Controls...")
    p5 = os.path.join(SRC_DIR, "05_telemetry_simulator.png")
    if os.path.exists(p5):
        img5 = Image.open(p5)
        # 2880x2692 -> 1440x1346
        fig6 = img5.resize((1440, 1346), Image.Resampling.LANCZOS)
        fig6 = add_border(fig6)
        save_dual(fig6, "fig06_telemetry_simulator_controls")

    # -------------------------------------------------------------
    # Fig 7: Mobile Responsive Pair (Dashboard + Driver Simulator)
    # -------------------------------------------------------------
    print("\n[8/8] Processing Fig 7: Mobile Responsive Layouts...")
    p6 = os.path.join(SRC_DIR, "06_mobile_dashboard.png")
    p7 = os.path.join(SRC_DIR, "07_mobile_driver_console.png")
    if os.path.exists(p6) and os.path.exists(p7):
        m_dash = Image.open(p6)
        m_driver = Image.open(p7)
        
        # Standard mobile viewport height (approx 1688px at 2x)
        view_h = 1688
        c_dash = m_dash.crop((0, 0, 780, min(m_dash.size[1], view_h)))
        c_driver = m_driver.crop((0, 0, 780, min(m_driver.size[1], view_h)))
        
        gap = 40
        m_canvas = Image.new("RGB", (780 * 2 + gap + 40, view_h + 40), color=(241, 245, 249))
        m_canvas.paste(add_border(c_dash), (20, 20))
        m_canvas.paste(add_border(c_driver), (780 + gap + 20, 20))
        
        # Scale to 1200 width
        fig7_h = int(m_canvas.size[1] * (1200 / m_canvas.size[0]))
        fig7 = m_canvas.resize((1200, fig7_h), Image.Resampling.LANCZOS)
        fig7 = add_border(fig7)
        save_dual(fig7, "fig07_mobile_responsive_views")

    # Clean up test scratch files
    for tmp in ["test_map.png", "test_eval.png", "test_selected.png", "test_composite_selection.png", "test_card_selected.png", "test_card_explain.png", "test_mobile_pair.png"]:
        tmp_p = os.path.join(OUT_DIR, tmp)
        if os.path.exists(tmp_p):
            os.remove(tmp_p)

    print("\nAll project report figures generated successfully in docs/images/!")

if __name__ == "__main__":
    main()
