package com.darktubbie.aeroplayer.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.darktubbie.aeroplayer.R

/**
 * Registro único de [AmbientEventType] (Fase 6 de ".aero", 0.5.0 —
 * mejor organización de efectos existentes).
 *
 * Antes de esta fase la relación entre un [AmbientEventType] y su
 * representación externa (nombre de texto en `.ape`, emoji, label
 * traducible) vivía repartida en varios `when` casi idénticos:
 * `ambientEventTypeKey` en `CustomEffectImage.kt`,
 * `ForegroundLayer.spawnApeEffect` (parseo de texto a tipo), y
 * `ApeEditorScreen.effectEmoji`/`effectLabel` + los 4
 * `EffectOption(...)` hardcodeados del selector. Agregar un tipo
 * nuevo significaba tocar los 4 lugares a mano, sin que el
 * compilador avisara si alguno quedaba desactualizado.
 *
 * Ahora cada relación tiene un único lugar:
 *
 * - Tipo -> nombre en `.ape`: [ambientEventTypeKey].
 * - Nombre en `.ape` -> tipo (la inversa exacta): [ambientEventTypeFromKey].
 *   Usada tanto por [ForegroundLayer] (interpretar un `.ape`) como
 *   por el editor (saber qué tipo seleccionó el usuario). Insensible
 *   a mayúsculas/espacios, igual que antes; un nombre no reconocido
 *   sigue devolviendo `null` sin romper nada (compatibilidad con
 *   `.ape` ya creados, incluso si mencionan un efecto que esta
 *   versión no conoce).
 * - Tipo -> emoji: [ambientEventTypeEmoji]. La usan tanto el editor
 *   (selector y lista de eventos) como, indirectamente, los
 *   composables de [AmbientEventShapes] (que dibujan el mismo emoji,
 *   pero como parte de un `Text` ya estilizado — no llaman a esta
 *   función, la mantienen en paralelo a propósito para no forzar un
 *   `Composable` en un sitio que hoy no lo necesita).
 * - Tipo -> label traducible: [ambientEventTypeLabel].
 *
 * Como todas están indexadas por [AmbientEventType] (no por String
 * suelto), agregar un tipo nuevo al enum hace que el compilador
 * marque como error cada `when` de este archivo que todavía no lo
 * cubre — ya no depende de acordarse.
 */
fun ambientEventTypeKey(
    type: AmbientEventType
): String =

    when (type) {
        AmbientEventType.BUBBLE_FRONT -> "bubble"
        AmbientEventType.FISH -> "fish"
        AmbientEventType.JELLYFISH -> "jellyfish"
        AmbientEventType.CLOUD -> "cloud"
        AmbientEventType.LEAF -> "leaf"
    }

/**
 * Inversa de [ambientEventTypeKey], insensible a
 * mayúsculas/espacios. "bubble_front" también resuelve a
 * [AmbientEventType.BUBBLE_FRONT] — alias histórico que ya aceptaba
 * el formato `.ape` antes de esta fase.
 */
fun ambientEventTypeFromKey(
    raw: String
): AmbientEventType? {

    val normalized =
        raw.trim().lowercase()

    if (normalized == "bubble_front") {
        return AmbientEventType.BUBBLE_FRONT
    }

    return AmbientEventType.entries.firstOrNull { type ->
        ambientEventTypeKey(type) == normalized
    }
}

fun ambientEventTypeEmoji(
    type: AmbientEventType
): String =

    when (type) {
        AmbientEventType.BUBBLE_FRONT -> "🫧"
        AmbientEventType.FISH -> "🐠"
        AmbientEventType.JELLYFISH -> "🪼"
        AmbientEventType.CLOUD -> "☁️"
        AmbientEventType.LEAF -> "🍃"
    }

@Composable
fun ambientEventTypeLabel(
    type: AmbientEventType
): String =

    when (type) {

        AmbientEventType.BUBBLE_FRONT ->
            stringResource(R.string.ape_effect_bubble)

        AmbientEventType.FISH ->
            stringResource(R.string.ape_effect_fish)

        AmbientEventType.JELLYFISH ->
            stringResource(R.string.ape_effect_jellyfish)

        AmbientEventType.CLOUD ->
            stringResource(R.string.ape_effect_cloud)

        AmbientEventType.LEAF ->
            stringResource(R.string.ape_effect_leaf)
    }
