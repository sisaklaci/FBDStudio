# FBD Studio

Professzionális **IEC 61131-3 Function Block Diagram (FBD)** szerkesztő,
tervező és szimulátor (JavaFX, Maven).

## Futtatás

```
java -jar target/fbd-studio-1.0.0-all.jar
```

(Java futtatókörnyezet szükséges a gépen — a jar az összes JavaFX
függőséget tartalmazza, "fat jar".)

### Fordítás forrásból

```
mvn clean package
```

(Lásd a [`build.ps1`](build.ps1) szkriptet natív Windows `.exe` csomag
készítéséhez beágyazott Java futtatókörnyezettel, jpackage-dzsel.)

## Főbb funkciók

- **Funkcióblokk-diagram szerkesztő**: blokkok elhelyezése, bekötése,
  IEC 61131-3 szemantika szerinti kiértékelés/szimuláció
- **Blokk-könyvtár kategóriák**: aritmetika, bistabil tárolók (SR/RS),
  összehasonlítás, számlálók, élfigyelők (R_TRIG/F_TRIG), kiválasztás,
  logika (AND/OR/NOT/XOR), időzítők, kiterjesztett blokkok
- **12 kész minta projekt** (`.fbdproj`): pl. villogó riasztó,
  vészleállítási lánc, parkolóház-számláló, PID hőmérséklet-szabályozás,
  csillag-delta indítás, tartály-hiszterézis, pergésmentesítő kapcsolás
- Többnyelvű felület: magyar, angol, német, francia, spanyol, lengyel,
  japán, kínai

## Mappaszerkezet

- [`src/main/java/`](src/main/java/com/sisak/fbdstudio) — forráskód
- [`src/main/resources/blocks/`](src/main/resources/blocks) — blokk-definíciók
  (JSON)
- [`src/main/resources/examples/`](src/main/resources/examples) — kész minta
  projektek
- [`src/main/resources/i18n/`](src/main/resources/i18n) — fordítások
- [`target/fbd-studio-1.0.0-all.jar`](target/fbd-studio-1.0.0-all.jar) —
  futtatható jar (az összes függőséggel)

## Licenc

A projekt a [PolyForm Noncommercial 1.0.0](LICENSE) licenc alatt áll: szabadon
nézhető, módosítható és terjeszthető, de csak nem kereskedelmi célra.
