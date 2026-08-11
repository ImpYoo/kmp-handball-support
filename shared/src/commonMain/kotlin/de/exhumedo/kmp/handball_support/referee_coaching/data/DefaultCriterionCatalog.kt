package de.exhumedo.kmp.handball_support.referee_coaching.data

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CriterionCategory
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.DefectGroup
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RootCause
import de.exhumedo.kmp.handball_support.referee_coaching.domain.repository.CriterionCatalogRepository

/**
 * In-code [CriterionCatalogRepository] holding the official HVNB
 * referee observation sheet ("Schiedsrichter-Beobachterbericht",
 * Handballverband Niedersachsen e.V.).
 *
 * Content is transcribed from `Neuer_Beobachtungsbogen_HVNB.pdf`:
 *  - Section A "Spielregeln" — criteria A1..A8
 *  - Section B "Persönlicher Eindruck" — criteria B1..B4
 *
 * Each criterion lists its Mängelhauptgruppen (defect groups) and the
 * ursächlichen Fehler (root causes). This is the single source of truth for the
 * catalog content and can later be swapped for a JSON-resource or server-backed
 * implementation without touching the domain or presentation layers.
 *
 * All entries are returned with `count = 0` and the default score, so callers
 * always receive a clean template to score against.
 */
class DefaultCriterionCatalog : CriterionCatalogRepository {

    override fun loadCriteria(): List<Criterion> = CRITERIA

    private companion object {

        val CRITERIA: List<Criterion> = listOf(
            // ---------------------------------------------------------------
            // A — Spielregeln
            // ---------------------------------------------------------------
            Criterion(
                id = "a1-spielgedanke-vorteil",
                name = "Spielgedanke / Vorteil",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a1-spielverstaendnis",
                        name = "Spielverständnis",
                        rootCauses = listOf(
                            RootCause(id = "a1-spiel-verstehen-lesen", name = "Spiel \"verstehen/lesen\""),
                            RootCause(id = "a1-schneller-anwurf", name = "\"schneller Anwurf\""),
                        ),
                    ),
                    DefectGroup(
                        id = "a1-entsch-geg-spielfluss",
                        name = "Entsch. geg. Spielfluss",
                        rootCauses = listOf(
                            RootCause(id = "a1-pfiff-spielaufbau", name = "Pfiff in Spielaufbau"),
                            RootCause(id = "a1-pfiff-torwurf", name = "Pfiff beim Torwurf"),
                        ),
                    ),
                    DefectGroup(
                        id = "a1-vorteil-mit-fehlern",
                        name = "Vorteil mit Fehlern",
                        rootCauses = listOf(
                            RootCause(id = "a1-vorteil-schritten", name = "Vorteil mit Schritten"),
                            RootCause(id = "a1-vorteil-uebertreten", name = "Vorteil mit Übertreten"),
                        ),
                    ),
                    DefectGroup(
                        id = "a1-verhaeltnis-vorteil-haerte",
                        name = "Verhältnis Vorteil/Härte",
                        rootCauses = listOf(
                            RootCause(id = "a1-vorteil-kosten-fairness", name = "Vorteil auf Kosten Fairness"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a2-stuermerfoul",
                name = "Stürmerfoul",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a2-sf-mit-ballbesitz",
                        name = "SF mit Ballbesitz",
                        rootCauses = listOf(
                            RootCause(id = "a2-anrennen-anspringen", name = "anrennen, anspringen"),
                            RootCause(id = "a2-einklemmen", name = "einklemmen"),
                        ),
                    ),
                    DefectGroup(
                        id = "a2-sf-ohne-ballbesitz",
                        name = "SF ohne Ballbesitz",
                        rootCauses = listOf(
                            RootCause(id = "a2-anrennen-nach-abspiel", name = "anrennen (nach Abspiel)"),
                            RootCause(id = "a2-am-torraum", name = "am Torraum"),
                        ),
                    ),
                    DefectGroup(
                        id = "a2-provozierte-stuermerfouls",
                        name = "Provozierte Stürmerfouls",
                        rootCauses = listOf(
                            RootCause(id = "a2-sf-zeitgewinn", name = "SF zum Zeitgewinn"),
                        ),
                    ),
                    DefectGroup(
                        id = "a2-torerfolg-mit-stf",
                        name = "Torerfolg mit StF",
                        rootCauses = listOf(
                            RootCause(id = "a2-nicht-erkannt", name = "nicht erkannt"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a3-progressivitaet-strafmass",
                name = "Progressivität / Strafmaß",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a3-progr-aufbau",
                        name = "progr. Aufbau",
                        rootCauses = listOf(
                            RootCause(id = "a3-progr-vorgabe", name = "progr. Vorgabe"),
                            RootCause(id = "a3-einhalten-eigener-linie", name = "Einhalten eigener Linie"),
                        ),
                    ),
                    DefectGroup(
                        id = "a3-progr-niveauansatz",
                        name = "progr. Niveauansatz",
                        rootCauses = listOf(
                            RootCause(id = "a3-zu-niedrig-grosszuegig", name = "zu niedrig/großzügig"),
                            RootCause(id = "a3-zu-hoch-ueberzogen", name = "zu hoch/überzogen"),
                        ),
                    ),
                    DefectGroup(
                        id = "a3-abstand-nachtr-strafe",
                        name = "Abstand / nachtr. Strafe",
                        rootCauses = listOf(
                            RootCause(id = "a3-abstand-freiwuerfen", name = "Abstand bei Freiwürfen"),
                            RootCause(id = "a3-vorteil-ohne-strafe", name = "Vorteil ohne erf. Strafe"),
                        ),
                    ),
                    DefectGroup(
                        id = "a3-progr-schwerpunkte",
                        name = "progr. Schwerpunkte",
                        rootCauses = listOf(
                            RootCause(id = "a3-trikotreissen", name = "Trikotreissen"),
                            RootCause(id = "a3-ringen-am-kreis", name = "\"Ringen\" am Kreis"),
                        ),
                    ),
                    DefectGroup(
                        id = "a3-disqualifikationen",
                        name = "Disqualifikationen",
                        rootCauses = listOf(
                            RootCause(id = "a3-disqualif-fehlt", name = "Disqualif. fehlt"),
                            RootCause(id = "a3-disqualif-unberechtigt", name = "Disqualif. unberechtigt"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a4-spielen-des-balles",
                name = "Spielen des Balles",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a4-schritte-linie",
                        name = "Schritte - Linie",
                        rootCauses = listOf(
                            RootCause(id = "a4-schwankende-linie", name = "schwankende Linie"),
                        ),
                    ),
                    DefectGroup(
                        id = "a4-schritte-anzahl",
                        name = "Schritte - Anzahl",
                        rootCauses = listOf(
                            RootCause(id = "a4-zu-grosszuegig", name = "zu großzügig (mehr als 3)"),
                            RootCause(id = "a4-zu-kleinlich", name = "zu kleinlich (weniger als 3)"),
                        ),
                    ),
                    DefectGroup(
                        id = "a4-fussfehler",
                        name = "Fußfehler",
                        rootCauses = listOf(
                            RootCause(id = "a4-fuss", name = "Fuß"),
                            RootCause(id = "a4-fuss-zur-abwehr", name = "Fuß zur Abw. (auch A3)"),
                        ),
                    ),
                    DefectGroup(
                        id = "a4-andere-fehler-mit-ball",
                        name = "andere Fehler mit Ball",
                        rootCauses = listOf(
                            RootCause(id = "a4-prellfehler", name = "Prellfehler"),
                            RootCause(id = "a4-zeitfehler", name = "Zeitfehler (3 Sekunden)"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a5-betreten-torraum",
                name = "Betreten Torraum",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a5-angreifer-ohne-torerfolg",
                        name = "Angreifer ohne Torerfolg",
                        rootCauses = listOf(
                            RootCause(id = "a5-hinterlaufen-tr-ohne", name = "Hinterlaufen durch TR"),
                        ),
                    ),
                    DefectGroup(
                        id = "a5-angreifer-mit-torerfolg",
                        name = "Angreifer mit Torerfolg",
                        rootCauses = listOf(
                            RootCause(id = "a5-be-uebertreten", name = "be-/übertreten"),
                            RootCause(id = "a5-abstehen", name = "abstehen"),
                        ),
                    ),
                    DefectGroup(
                        id = "a5-abwehr-im-torraum",
                        name = "Abwehr im Torraum",
                        rootCauses = listOf(
                            RootCause(id = "a5-abwehrarbeit-im-tr", name = "Abwehrarbeit im TR"),
                            RootCause(id = "a5-hinterlaufen-tr-abwehr", name = "Hinterlaufen durch TR"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a6-siebenmeter",
                name = "Siebenmeter",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a6-7m-entscheidungen",
                        name = "7-m-Entscheidungen",
                        rootCauses = listOf(
                            RootCause(id = "a6-unklare-linie", name = "unklare Linie"),
                        ),
                    ),
                    DefectGroup(
                        id = "a6-7m-niveauansatz",
                        name = "7-m (Niveauansatz)",
                        rootCauses = listOf(
                            RootCause(id = "a6-zu-viel", name = "zu viel"),
                            RootCause(id = "a6-zu-wenig", name = "zu wenig"),
                        ),
                    ),
                    DefectGroup(
                        id = "a6-7m-klare-torgelegenheit",
                        name = "7-m: klare Torgelegenh.",
                        rootCauses = listOf(
                            RootCause(id = "a6-behinderung-freier-werfer", name = "Behinderung eines vll. freien Werfers"),
                            RootCause(id = "a6-betreten-torraum-gegen-werfer", name = "Betreten Torraum gegen den Werfer"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a7-passives-spiel",
                name = "Passives Spiel",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a7-passive-linie",
                        name = "passive Linie",
                        rootCauses = listOf(
                            RootCause(id = "a7-schwankend", name = "schwankend"),
                            RootCause(id = "a7-bei-unterzahl", name = "bei Unterzahl"),
                        ),
                    ),
                    DefectGroup(
                        id = "a7-einsatz-handzeichen-passiv",
                        name = "Einsatz Handz. \"passiv\"",
                        rootCauses = listOf(
                            RootCause(id = "a7-hz-zu-frueh", name = "Hz zu früh"),
                            RootCause(id = "a7-hz-nicht-zu-spaet", name = "Hz nicht oder zu spät"),
                        ),
                    ),
                    DefectGroup(
                        id = "a7-entscheidung-passiv",
                        name = "Entscheidung \"passiv\"",
                        rootCauses = listOf(
                            RootCause(id = "a7-entscheidung-zu-frueh", name = "Entscheidung zu früh"),
                            RootCause(id = "a7-entscheidung-nicht-zu-spaet", name = "Entsch. nicht / zu spät"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "a8-weitere-regeln",
                name = "Weitere Regeln",
                category = CriterionCategory.RULES_OF_THE_GAME,
                defectGroups = listOf(
                    DefectGroup(
                        id = "a8-spielzeit",
                        name = "Spielzeit",
                        rootCauses = listOf(
                            RootCause(id = "a8-time-out-team-to", name = "Time-out; Team-T-o."),
                            RootCause(id = "a8-puenktlicher-spielbeginn", name = "pünktlicher Spielbeginn"),
                        ),
                    ),
                    DefectGroup(
                        id = "a8-ordnungsprinzip",
                        name = "Ordnungsprinzip",
                        rootCauses = listOf(
                            RootCause(id = "a8-aufstellungsformen", name = "Aufstellungsformen"),
                        ),
                    ),
                    DefectGroup(
                        id = "a8-wurfentscheidung",
                        name = "Wurfentscheidung",
                        rootCauses = listOf(
                            RootCause(id = "a8-falscher-wurf-mannschaft", name = "falsche(r) Wurf/Mannsch."),
                            RootCause(id = "a8-falscher-ort", name = "falscher Ort"),
                        ),
                    ),
                    DefectGroup(
                        id = "a8-wurfausfuehrung",
                        name = "Wurfausführung",
                        rootCauses = listOf(
                            RootCause(id = "a8-fw-linie-betreten", name = "FW-Linie betreten"),
                            RootCause(id = "a8-im-lauf-sprung", name = "im Lauf / Sprung"),
                        ),
                    ),
                ),
            ),

            // ---------------------------------------------------------------
            // B — Persönlicher Eindruck
            // ---------------------------------------------------------------
            Criterion(
                id = "b1-persoenlichkeit-der-sr",
                name = "Persönlichkeit der SR",
                category = CriterionCategory.PERSONAL_IMPRESSION,
                defectGroups = listOf(
                    DefectGroup(
                        id = "b1-die-person-sr",
                        name = "die Person SR",
                        rootCauses = listOf(
                            RootCause(id = "b1-unnatuerlich-ueberheblich", name = "unnatürlich/überheblich"),
                            RootCause(id = "b1-nervoes-unsouveraen", name = "nervös/unsouverän"),
                        ),
                    ),
                    DefectGroup(
                        id = "b1-auftreten-der-sr",
                        name = "Auftreten der/des SR",
                        rootCauses = listOf(
                            RootCause(id = "b1-zaghaft-unsicher", name = "zaghaft/unsicher"),
                            RootCause(id = "b1-beeinflussbar", name = "beeinflußbar"),
                        ),
                    ),
                    DefectGroup(
                        id = "b1-koerperl-geist-bereitschaft",
                        name = "körperl./geist. Bereitschaft",
                        rootCauses = listOf(
                            RootCause(id = "b1-mangelnde-athletik", name = "mangelnde Athletik"),
                            RootCause(id = "b1-nachlassende-konzentration", name = "nachlassende Konzentr."),
                        ),
                    ),
                    DefectGroup(
                        id = "b1-koerpersprache",
                        name = "Körpersprache",
                        rootCauses = listOf(
                            RootCause(id = "b1-kein-selbstbewusstes-auftreten", name = "kein selbstbew. Auftreten"),
                            RootCause(id = "b1-schuechtern", name = "schüchtern"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "b2-zusammenarbeit-der-sr",
                name = "Zusammenarbeit der SR",
                category = CriterionCategory.PERSONAL_IMPRESSION,
                defectGroups = listOf(
                    DefectGroup(
                        id = "b2-teamarbeit",
                        name = "Teamarbeit",
                        rootCauses = listOf(
                            RootCause(id = "b2-kein-geschlossenes-team", name = "kein geschloss. Team"),
                            RootCause(id = "b2-dominanz-eines-sr", name = "Dominanz eines SR"),
                        ),
                    ),
                    DefectGroup(
                        id = "b2-stellungsspiel",
                        name = "Stellungsspiel",
                        rootCauses = listOf(
                            RootCause(id = "b2-unguenstige-beob-position", name = "ungünst. Beob-Position"),
                            RootCause(id = "b2-mangel-bei-abstimmung", name = "Mangel bei Abstimmung"),
                        ),
                    ),
                    DefectGroup(
                        id = "b2-aufgabenteilung",
                        name = "Aufgabenteilung",
                        rootCauses = listOf(
                            RootCause(id = "b2-aufgabenbereiche", name = "Aufgabenbereiche"),
                            RootCause(id = "b2-fsr-tsr", name = "FSR/TSR"),
                        ),
                    ),
                    DefectGroup(
                        id = "b2-zusammenarbeit-sr-zs",
                        name = "Zusammenarb. SR/Z-S",
                        rootCauses = listOf(
                            RootCause(id = "b2-keine-unkorrekte-zeichen", name = "keine / unkorr. Zeichen"),
                            RootCause(id = "b2-weitere-abstimmungsprobleme", name = "weitere Abstimm.-Probl."),
                        ),
                    ),
                    DefectGroup(
                        id = "b2-bankverhalten",
                        name = "Bankverhalten",
                        rootCauses = listOf(
                            RootCause(id = "b2-keine-zu-viele-ermahnungen", name = "keine/ zu viele Ermahn."),
                            RootCause(id = "b2-baenke-unterschiedlich", name = "Bänke unterschiedlich"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "b3-einflussnahme-kommunikation",
                name = "Einflussnahme / Kommunikation",
                category = CriterionCategory.PERSONAL_IMPRESSION,
                defectGroups = listOf(
                    DefectGroup(
                        id = "b3-optische-signale",
                        name = "opt. Signale (Handzeichen)",
                        rootCauses = listOf(
                            RootCause(id = "b3-keine-handzeichen", name = "keine Handzeichen"),
                            RootCause(id = "b3-unverstaendliche-handzeichen", name = "unverständliche Handz."),
                        ),
                    ),
                    DefectGroup(
                        id = "b3-akustische-signale",
                        name = "Akustische Signale (Pfiffe)",
                        rootCauses = listOf(
                            RootCause(id = "b3-zu-monoton", name = "zu monoton"),
                            RootCause(id = "b3-zu-leise", name = "zu leise"),
                        ),
                    ),
                    DefectGroup(
                        id = "b3-gestik",
                        name = "Gestik",
                        rootCauses = listOf(
                            RootCause(id = "b3-zu-theatralisch", name = "zu theatralisch"),
                            RootCause(id = "b3-gestikulieren", name = "gestikulieren"),
                        ),
                    ),
                ),
            ),
            Criterion(
                id = "b4-spielleitung-insgesamt",
                name = "Spielleitung, insgesamt",
                category = CriterionCategory.PERSONAL_IMPRESSION,
                defectGroups = listOf(
                    DefectGroup(
                        id = "b4-gesamtlinie-der-sr",
                        name = "Gesamtlinie der SR",
                        rootCauses = listOf(
                            RootCause(id = "b4-zu-grosszuegig", name = "zu \"großzügig\""),
                            RootCause(id = "b4-zu-kleinlich", name = "zu \"kleinlich\""),
                        ),
                    ),
                    DefectGroup(
                        id = "b4-unterschiedliche-halbzeiten",
                        name = "unterschiedliche Halbz.",
                        rootCauses = listOf(
                            RootCause(id = "b4-erste-halbzeit-schwaecher", name = "1. Halbzeit schwächer"),
                            RootCause(id = "b4-zweite-halbzeit-schwaecher", name = "2. Halbzeit schwächer"),
                        ),
                    ),
                    DefectGroup(
                        id = "b4-gleichbehandlung",
                        name = "Gleichbehandlung",
                        rootCauses = listOf(
                            RootCause(id = "b4-gesamte-spielzeit", name = "gesamte Spielzeit"),
                            RootCause(id = "b4-zum-spielschluss", name = "zum Spielschluss"),
                        ),
                    ),
                    DefectGroup(
                        id = "b4-beeinflusst-durch-a-b",
                        name = "beeinflusst durch A/B",
                        rootCauses = listOf(
                            RootCause(id = "b4-schwerpunkt-regelwerk", name = "Schwerpunkt Regelwerk"),
                            RootCause(id = "b4-schwerpunkt-persoenliches", name = "Schwerpunkt Persönliches"),
                        ),
                    ),
                ),
            ),
        )
    }
}


