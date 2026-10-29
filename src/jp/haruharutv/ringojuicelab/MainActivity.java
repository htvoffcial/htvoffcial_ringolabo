package jp.haruharutv.ringojuicelab;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    // --- simulation state ---
    double ph = 3.6;          // apple juice is acidic
    double temp = 20.0;       // celsius
    int volumeMl = 200;
    int bubbles = 0;          // 0..100
    int turbidity = 0;        // 0..100 cloudy
    int indicatorMode = 0;    // 0=none 1=purple cabbage 2=BTB 3=phenolphthalein
    boolean iodine = false;
    boolean nitric = false;
    boolean starch = false;
    boolean lime = false;         // limewater added
    boolean co2 = false;          // dissolved carbon dioxide present
    boolean chloride = false;     // chloride ions (from salt)
    boolean silver = false;       // silver nitrate added
    boolean copper = false;       // copper sulfate (blue Cu2+)
    boolean cuPrecip = false;     // Cu(OH)2 precipitate
    boolean deepCopper = false;   // tetraamminecopper(II) deep blue
    boolean permanganate = false; // KMnO4 purple
    boolean iron = false;
    int strongDoses = 0;
    int doses = 0;
    final List<String> logLines = new ArrayList<>();

    JuiceView juiceView;
    TextView readout;
    TextView logView;

    static class JuiceView extends View {
        double ph = 3.6;
        int bubbles = 0;
        int turbidity = 0;
        int indicatorMode = 0;
        boolean iodine = false;
        boolean nitric = false;
        boolean starch = false;
        boolean copper = false;
        boolean cuPrecip = false;
        boolean deepCopper = false;
        boolean permanganate = false;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rnd = new Random(42);

        JuiceView(android.content.Context c) { super(c); }

        int liquidColor() {
            if (permanganate) return Color.rgb(122, 32, 142);      // KMnO4 purple
            if (starch && iodine) return Color.rgb(35, 45, 130);   // iodine-starch: blue-purple
            if (deepCopper) return Color.rgb(28, 58, 175);         // tetraamminecopper(II)
            if (cuPrecip) return Color.rgb(150, 185, 225);         // Cu(OH)2 suspension
            if (copper) return Color.rgb(70, 120, 205);            // Cu2+ blue
            if (indicatorMode == 1) {                              // purple cabbage
                if (ph < 3.0) return Color.rgb(232, 72, 110);      // pink-red (strong acid)
                if (ph < 5.0) return Color.rgb(196, 84, 160);      // reddish purple
                if (ph < 6.5) return Color.rgb(128, 90, 190);      // purple
                if (ph < 8.0) return Color.rgb(70, 120, 210);      // blue
                if (ph < 10.0) return Color.rgb(70, 170, 120);     // blue-green
                return Color.rgb(170, 190, 60);                    // yellow-green (alkaline)
            }
            if (indicatorMode == 2) {                              // BTB
                if (ph < 6.0) return Color.rgb(235, 205, 60);      // yellow (acid)
                if (ph < 7.6) return Color.rgb(90, 175, 95);       // green (neutral)
                return Color.rgb(65, 115, 215);                    // blue (alkaline)
            }
            if (indicatorMode == 3 && ph >= 8.3) {
                return Color.rgb(242, 120, 175);                   // phenolphthalein pink
            }
            if (iodine) return Color.rgb(146, 92, 32);             // brownish
            if (nitric) return Color.rgb(226, 176, 52);             // yellowed by nitric acid
            // plain apple juice: golden, a bit paler when very acidic
            int r = 244, g = 183, b = 64;
            if (turbidity > 40) { r = 232; g = 190; b = 110; }
            return Color.rgb(r, g, b);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            // background
            paint.setColor(Color.rgb(245, 247, 250));
            canvas.drawRect(0, 0, w, h, paint);

            float gx0 = w * 0.22f, gx1 = w * 0.78f;
            float top = h * 0.08f, bottom = h * 0.92f;
            float bx0 = w * 0.28f, bx1 = w * 0.72f;

            // liquid (clip-ish trapezoid)
            Path liquid = new Path();
            float ly = top + (bottom - top) * 0.25f;
            liquid.moveTo(gx0 + (bx0 - gx0) * 0.25f, ly);
            liquid.lineTo(gx1 - (gx1 - bx1) * 0.25f, ly);
            liquid.lineTo(bx1, bottom);
            liquid.lineTo(bx0, bottom);
            liquid.close();
            paint.setColor(liquidColor());
            canvas.drawPath(liquid, paint);

            // cloudiness overlay
            if (turbidity > 0) {
                paint.setColor(Color.argb(Math.min(140, turbidity * 2), 255, 255, 255));
                canvas.drawPath(liquid, paint);
            }

            // bubbles
            if (bubbles > 0) {
                paint.setColor(Color.argb(180, 255, 255, 255));
                rnd.setSeed(1234);
                int n = bubbles / 2;
                for (int i = 0; i < n; i++) {
                    float bx = bx0 + rnd.nextFloat() * (bx1 - bx0);
                    float by = ly + 20 + rnd.nextFloat() * (bottom - ly - 30);
                    float br = 3 + rnd.nextFloat() * 8;
                    canvas.drawCircle(bx, by, br, paint);
                }
            }

            // glass outline
            Path glass = new Path();
            glass.moveTo(gx0, top);
            glass.lineTo(gx1, top);
            glass.lineTo(bx1, bottom);
            glass.lineTo(bx0, bottom);
            glass.close();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setColor(Color.rgb(90, 110, 130));
            canvas.drawPath(glass, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("🍎 りんごジュース実験室");
        title.setTextSize(24);
        root.addView(title);

        TextView safety = new TextView(this);
        safety.setText("※ これは画面の中だけの仮想実験です。実際の飲み物に薬品を入れたり、混ぜたものを飲んだりしないでください。");
        safety.setTextSize(13);
        safety.setTextColor(Color.rgb(180, 40, 40));
        root.addView(safety);

        juiceView = new JuiceView(this);
        juiceView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 640));
        root.addView(juiceView);

        readout = new TextView(this);
        readout.setTextSize(17);
        root.addView(readout);

        TextView chemLabel = new TextView(this);
        chemLabel.setText("― 薬品を入れる（1タップ＝1はい）―");
        chemLabel.setTextSize(16);
        root.addView(chemLabel);

        String[][] chems = {
            {"🍋 クエン酸", "acid"},
            {"🧂 重曹", "soda"},
            {"🟣 紫キャベツ液\n（指示薬）", "indicator"},
            {"🟡 BTB液\n（指示薬）", "btb"},
            {"🌸 フェノールフタレイン液\n（指示薬）", "pp"},
            {"🟤 ヨウ素液", "iodine"},
            {"🌾 デンプン液", "starch"},
            {"🥛 石灰水", "limewater"},
            {"🫧 炭酸水", "sparkling"},
            {"🧊 お湯", "hotwater"},
            {"🍬 砂糖", "sugar"},
            {"🧂 食塩", "salt"},
            {"🧲 鉄粉", "iron"},
        };
        for (int i = 0; i < chems.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, chems.length); j++) {
                final String key = chems[j][1];
                Button b = new Button(this);
                b.setText(chems[j][0]);
                b.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                b.setOnClickListener(v -> addChemical(key));
                row.addView(b);
            }
            root.addView(row);
        }

        TextView strongLabel = new TextView(this);
        strongLabel.setText("― 劇薬・強めの試薬（仮想のみ）―");
        strongLabel.setTextSize(16);
        strongLabel.setTextColor(Color.rgb(180, 40, 40));
        root.addView(strongLabel);

        TextView strongNote = new TextView(this);
        strongNote.setText("強い酸・アルカリの反応を画面の中で再現したものです。現実で試したり、飲み物に入れたりしないでください。");
        strongNote.setTextSize(12);
        strongNote.setTextColor(Color.rgb(180, 40, 40));
        root.addView(strongNote);

        String[][] strong = {
            {"☠️ 硫酸（強酸）", "sulfuric"},
            {"☠️ 硝酸（強酸）", "nitric"},
            {"⚠️ 塩酸（強酸）", "hydrochloric"},
            {"☠️ 水酸化ナトリウム\n（強アルカリ）", "naoh"},
            {"🫧 過酸化水素水", "peroxide"},
            {"⚠️ アンモニア水", "ammonia"},
            {"🔵 硫酸銅", "copper"},
            {"⚪ 硝酸銀", "silver"},
            {"🟣 過マンガン酸カリウム", "permanganate"},
        };
        for (int i = 0; i < strong.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, strong.length); j++) {
                final String key = strong[j][1];
                Button b = new Button(this);
                b.setText(strong[j][0]);
                b.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                b.setOnClickListener(v -> addChemical(key));
                row.addView(b);
            }
            root.addView(row);
        }

        TextView actLabel = new TextView(this);
        actLabel.setText("― 操作 ―");
        actLabel.setTextSize(16);
        root.addView(actLabel);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        String[][] acts = {{"🥄 かき混ぜる", "stir"}, {"🔥 加熱", "heat"}, {"↺ リセット", "reset"}};
        for (String[] a : acts) {
            final String key = a[1];
            Button b = new Button(this);
            b.setText(a[0]);
            b.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            b.setOnClickListener(v -> doAction(key));
            actions.addView(b);
        }
        root.addView(actions);

        TextView logLabel = new TextView(this);
        logLabel.setText("― 実験ログ ―");
        logLabel.setTextSize(16);
        root.addView(logLabel);

        logView = new TextView(this);
        logView.setTextSize(14);
        root.addView(logView);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        log("実験スタート：りんごジュース 200mL（pH 3.6）");
        refresh();
    }

    void log(String s) {
        logLines.add(0, "・" + s);
        if (logLines.size() > 12) logLines.remove(logLines.size() - 1);
    }

    void addChemical(String key) {
        doses++;
        volumeMl += 10;
        switch (key) {
            case "acid":
                ph = Math.max(1.0, ph - 0.6);
                log("クエン酸を入れた → 酸性が強くなった");
                break;
            case "soda":
                if (ph < 5.5) {
                    bubbles = Math.min(100, bubbles + 65);
                    co2 = true;
                    log("重曹を入れた → 酸と反応してシュワシュワ発泡！（H⁺ + HCO₃⁻ → H₂O + CO₂↑）");
                } else {
                    log("重曹を入れた → あまり泡立たない（酸が弱い）");
                }
                ph = Math.min(13.0, ph + 0.8);
                if (lime && co2) {
                    turbidity = Math.min(100, turbidity + 20);
                    log("　石灰水が白くにごった → Ca(OH)₂ + CO₂ → CaCO₃↓ + H₂O");
                }
                break;
            case "indicator":
                indicatorMode = 1;
                log("紫キャベツ液を入れた → pHで色が変わるようになった（アントシアニン）");
                break;
            case "btb":
                indicatorMode = 2;
                log("BTB液を入れた → 酸性で黄色、中性で緑色、アルカリ性で青色に変わる");
                break;
            case "pp":
                indicatorMode = 3;
                log("フェノールフタレイン液を入れた → アルカリ性（pH 8.3以上）でピンク色に変わる");
                break;
            case "iodine":
                iodine = true;
                if (starch) {
                    log("ヨウ素液を入れた → ヨウ素デンプン反応で青紫色になった！");
                } else {
                    log("ヨウ素液を入れた → 茶色っぽくなった（デンプンがあると青紫色になる）");
                }
                turbidity = Math.min(100, turbidity + 20);
                break;
            case "starch":
                starch = true;
                if (iodine) {
                    log("デンプン液を入れた → ヨウ素デンプン反応で青紫色になった！");
                } else {
                    log("デンプン液を入れた → ヨウ素液と出会うと青紫色に変わる（ヨウ素デンプン反応）");
                }
                break;
            case "limewater":
                lime = true;
                ph = Math.min(13.0, ph + 0.7);
                if (co2) {
                    turbidity = Math.min(100, turbidity + 30);
                    co2 = false;
                    log("石灰水を入れた → 白くにごった！ Ca(OH)₂ + CO₂ → CaCO₃↓ + H₂O");
                } else {
                    log("石灰水を入れた → 二酸化炭素と反応すると白くにごる（炭酸カルシウムの沈殿）");
                }
                break;
            case "sparkling":
                bubbles = Math.min(100, bubbles + 30);
                ph = Math.max(1.0, ph - 0.2);
                co2 = true;
                log("炭酸水を入れた → 泡が出て、少し酸性に");
                if (lime) {
                    turbidity = Math.min(100, turbidity + 30);
                    co2 = false;
                    log("　石灰水が白くにごった → Ca(OH)₂ + CO₂ → CaCO₃↓ + H₂O");
                }
                break;
            case "hotwater":
                temp = Math.min(95.0, temp + 15);
                log("お湯を入れた → 温度が上がった");
                break;
            case "sugar":
                turbidity = Math.min(100, turbidity + 5);
                log("砂糖を入れた → 溶けて見た目はほぼ変化なし（甘さアップ）");
                break;
            case "salt":
                chloride = true;
                if (silver) {
                    turbidity = Math.min(100, turbidity + 45);
                    log("食塩を入れた → 白い沈殿ができた！ Ag⁺ + Cl⁻ → AgCl↓");
                } else {
                    log("食塩を入れた → 溶けて見た目は変化なし（塩化物イオン Cl⁻ が増えた）");
                }
                break;
            case "iron":
                iron = true;
                turbidity = Math.min(100, turbidity + 10);
                if (copper) {
                    copper = false;
                    log("鉄粉を入れた → 置換反応！鉄の表面に銅が析出して、青色が薄れた（Fe + CuSO₄ → FeSO₄ + Cu）");
                } else if (ph < 4.0) {
                    bubbles = Math.min(100, bubbles + 20);
                    log("鉄粉を入れた → 酸に溶けて水素の泡が出た！（Fe + 2H⁺ → Fe²⁺ + H₂↑）");
                } else {
                    log("鉄粉を入れた → 沈んだ（酸が強いと溶けて水素が出る／硫酸銅があると置換反応）");
                }
                break;
            case "sulfuric":
                strongDoses++;
                if (ph > 11) { temp = Math.min(98, temp + 18); bubbles = Math.min(100, bubbles + 40); log("硫酸を入れた → 強アルカリと激しく中和して発熱・発泡！⚠️（中和熱 約57 kJ/mol）"); }
                else log("硫酸を入れた → 一気に強い酸性に！熱も出てきた（仮想）");
                ph = Math.max(0.0, ph - 2.4);
                temp = Math.min(98, temp + 10);
                turbidity = Math.min(100, turbidity + 15);
                break;
            case "nitric":
                strongDoses++;
                nitric = true;
                if (ph > 11) { temp = Math.min(98, temp + 15); log("硝酸を入れた → 中和で発熱！液が黄色っぽくなった⚠️（中和熱 約57 kJ/mol）"); }
                else log("硝酸を入れた → 強い酸性に。液が黄色っぽく変色（仮想）");
                ph = Math.max(0.0, ph - 2.0);
                bubbles = Math.min(100, bubbles + 20);
                break;
            case "hydrochloric":
                strongDoses++;
                if (ph < 5.5 && bubbles < 90) bubbles = Math.min(100, bubbles + 15);
                log("塩酸を入れた → 一気に酸性が強くなった（仮想）");
                ph = Math.max(0.0, ph - 1.9);
                break;
            case "naoh":
                strongDoses++;
                if (copper && !cuPrecip) {
                    cuPrecip = true;
                    turbidity = Math.min(100, turbidity + 30);
                    ph = Math.min(14.0, ph + 2.5);
                    temp = Math.min(98, temp + 6);
                    log("水酸化ナトリウムを入れた → 青い沈殿ができた！ CuSO₄ + 2NaOH → Cu(OH)₂↓ + Na₂SO₄");
                    break;
                }
                if (ph < 3) { temp = Math.min(98, temp + 20); bubbles = Math.min(100, bubbles + 35); log("水酸化ナトリウムを入れた → 強い酸と中和して大きく発熱！⚠️（中和熱 約57 kJ/mol）"); }
                else log("水酸化ナトリウムを入れた → 一気に強いアルカリ性に！（仮想）");
                ph = Math.min(14.0, ph + 2.5);
                temp = Math.min(98, temp + 6);
                break;
            case "peroxide":
                if (permanganate) {
                    permanganate = false;
                    bubbles = Math.min(100, bubbles + 55);
                    log("過酸化水素水を入れた → 紫色が消えた！過マンガン酸が還元されて酸素が発生（2KMnO₄ + 5H₂O₂ + 3H₂SO₄ → K₂SO₄ + 2MnSO₄ + 8H₂O + 5O₂↑）");
                    break;
                }
                bubbles = Math.min(100, bubbles + 45);
                turbidity = Math.max(0, turbidity - 10);
                log("過酸化水素水を入れた → 酸素の泡がたくさん出た（仮想）");
                break;
            case "ammonia":
                strongDoses++;
                if (cuPrecip) {
                    cuPrecip = false;
                    deepCopper = true;
                    turbidity = Math.max(0, turbidity - 20);
                    ph = Math.min(14.0, ph + 1.2);
                    log("アンモニア水を入れた → 青い沈殿が溶けて、濃い青色になった！（テトラアンミン銅(II)イオン [Cu(NH₃)₄]²⁺）");
                    break;
                }
                ph = Math.min(14.0, ph + 1.2);
                log("アンモニア水を入れた → アルカリ性に。ツンとした臭いがする（はず）（仮想）");
                break;
            case "copper":
                strongDoses++;
                if (iron) {
                    log("硫酸銅を入れた → 置換反応！鉄の表面に銅が析出（Fe + CuSO₄ → FeSO₄ + Cu）");
                } else {
                    copper = true;
                    log("硫酸銅を入れた → 青い液になった（銅(II)イオン Cu²⁺ の色）");
                }
                break;
            case "silver":
                strongDoses++;
                silver = true;
                if (chloride) {
                    turbidity = Math.min(100, turbidity + 45);
                    log("硝酸銀を入れた → 白い沈殿ができた！ Ag⁺ + Cl⁻ → AgCl↓");
                } else {
                    log("硝酸銀を入れた → 塩化物イオン（食塩など）があると白い沈殿ができる");
                }
                break;
            case "permanganate":
                strongDoses++;
                permanganate = true;
                log("過マンガン酸カリウムを入れた → 紫色になった（強い酸化剤／過酸化水素水で脱色する）");
                break;
        }
        refresh();
    }

    void doAction(String key) {
        switch (key) {
            case "stir":
                bubbles = bubbles / 2;
                if (temp > 20) temp -= 3;
                log("かき混ぜた → 全体が均一になって、泡が少し消えた");
                break;
            case "heat":
                temp = Math.min(98.0, temp + 20);
                if (temp > 60) {
                    bubbles = Math.min(100, bubbles + 25);
                    log("加熱した → 温度が上がって、湯気と小さな泡が出てきた");
                } else {
                    log("加熱した → 少し温まった");
                }
                break;
            case "reset":
                ph = 3.6; temp = 20.0; volumeMl = 200; bubbles = 0; turbidity = 0;
                indicatorMode = 0; iodine = false; nitric = false;
                starch = false; lime = false; co2 = false; chloride = false; silver = false;
                copper = false; cuPrecip = false; deepCopper = false; permanganate = false; iron = false;
                strongDoses = 0; doses = 0;
                logLines.clear();
                log("リセット：新しいりんごジュース 200mL（pH 3.6）");
                break;
        }
        refresh();
    }

    String phLabel() {
        if (ph < 3) return "強い酸性";
        if (ph < 6) return "酸性";
        if (ph < 8) return "中性に近い";
        if (ph < 11) return "アルカリ性";
        return "強いアルカリ性";
    }

    void refresh() {
        juiceView.ph = ph;
        juiceView.bubbles = bubbles;
        juiceView.turbidity = turbidity;
        juiceView.indicatorMode = indicatorMode;
        juiceView.iodine = iodine;
        juiceView.nitric = nitric;
        juiceView.starch = starch;
        juiceView.copper = copper;
        juiceView.cuPrecip = cuPrecip;
        juiceView.deepCopper = deepCopper;
        juiceView.permanganate = permanganate;
        juiceView.invalidate();

        String hazard = strongDoses == 0 ? "なし" : (strongDoses < 3 ? "注意" : "危険！換気＆保護具（仮想）");
        readout.setText(String.format(
                "pH %.1f（%s）　［H⁺］%.1e mol/L　温度 %.0f℃　量 %dmL　入れた回数 %d回\n泡の量 %d%%　にごり %d%%　劇薬使用 %d回（警告レベル：%s）",
                ph, phLabel(), Math.pow(10, -ph), temp, volumeMl, doses, bubbles, turbidity, strongDoses, hazard));

        StringBuilder sb = new StringBuilder();
        for (String s : logLines) sb.append(s).append("\n");
        logView.setText(sb.toString());
    }
}
