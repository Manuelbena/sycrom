

import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import java.util.Locale

data class GoalSummaryState(
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val goals: List<GoalPresentationModel> = emptyList()
) {
    val totalPercent: Int get() = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0
    val formattedTotalSaved: String get() = String.format(Locale.getDefault(), "%,.2f €", totalSaved)
    val formattedTotalTarget: String get() = String.format(Locale.getDefault(), "de %,.2f €", totalTarget)
}