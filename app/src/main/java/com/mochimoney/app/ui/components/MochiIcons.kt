package com.mochimoney.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.mochimoney.app.ui.CategoryKind

object MochiIcons {
    val Dashboard = icon("Dashboard") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 5f)
            quadTo(4f, 3f, 6f, 3f)
            horizontalLineTo(10f)
            quadTo(12f, 3f, 12f, 5f)
            verticalLineTo(11f)
            quadTo(12f, 13f, 10f, 13f)
            horizontalLineTo(6f)
            quadTo(4f, 13f, 4f, 11f)
            close()
            moveTo(14f, 5f)
            quadTo(14f, 3f, 16f, 3f)
            horizontalLineTo(18f)
            quadTo(20f, 3f, 20f, 5f)
            verticalLineTo(8f)
            quadTo(20f, 10f, 18f, 10f)
            horizontalLineTo(16f)
            quadTo(14f, 10f, 14f, 8f)
            close()
            moveTo(14f, 14f)
            quadTo(14f, 12f, 16f, 12f)
            horizontalLineTo(18f)
            quadTo(20f, 12f, 20f, 14f)
            verticalLineTo(19f)
            quadTo(20f, 21f, 18f, 21f)
            horizontalLineTo(16f)
            quadTo(14f, 21f, 14f, 19f)
            close()
            moveTo(4f, 17f)
            quadTo(4f, 15f, 6f, 15f)
            horizontalLineTo(10f)
            quadTo(12f, 15f, 12f, 17f)
            verticalLineTo(19f)
            quadTo(12f, 21f, 10f, 21f)
            horizontalLineTo(6f)
            quadTo(4f, 21f, 4f, 19f)
            close()
        }
    }

    val Transactions = icon("Transactions") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            quadTo(21f, 5f, 21f, 7f)
            verticalLineTo(17f)
            quadTo(21f, 19f, 19f, 19f)
            horizontalLineTo(5f)
            quadTo(3f, 19f, 3f, 17f)
            verticalLineTo(7f)
            quadTo(3f, 5f, 5f, 5f)
            close()
            moveTo(5f, 8f)
            verticalLineTo(10f)
            horizontalLineTo(19f)
            verticalLineTo(8f)
            close()
            moveTo(7f, 14f)
            horizontalLineTo(12f)
            verticalLineTo(16f)
            horizontalLineTo(7f)
            close()
        }
    }

    val Categories = icon("Categories") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 3f)
            lineTo(21f, 13f)
            lineTo(13f, 21f)
            lineTo(3f, 11f)
            verticalLineTo(3f)
            close()
            moveTo(7.5f, 8.7f)
            quadTo(8.7f, 8.7f, 8.7f, 7.5f)
            quadTo(8.7f, 6.3f, 7.5f, 6.3f)
            quadTo(6.3f, 6.3f, 6.3f, 7.5f)
            quadTo(6.3f, 8.7f, 7.5f, 8.7f)
            close()
        }
    }

    val Settings = icon("Settings") {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 2.5f)
            lineTo(14.1f, 4.1f)
            lineTo(16.6f, 3.6f)
            lineTo(18.2f, 6.4f)
            lineTo(16.7f, 8.3f)
            quadTo(17.1f, 9.2f, 17.2f, 10.1f)
            lineTo(19.5f, 11.3f)
            verticalLineTo(14.5f)
            lineTo(17.2f, 15.7f)
            quadTo(17f, 16.6f, 16.6f, 17.4f)
            lineTo(18f, 19.4f)
            lineTo(16.4f, 22f)
            lineTo(13.9f, 21.5f)
            lineTo(12f, 23f)
            lineTo(10.1f, 21.5f)
            lineTo(7.6f, 22f)
            lineTo(6f, 19.4f)
            lineTo(7.4f, 17.4f)
            quadTo(7f, 16.6f, 6.8f, 15.7f)
            lineTo(4.5f, 14.5f)
            verticalLineTo(11.3f)
            lineTo(6.8f, 10.1f)
            quadTo(6.9f, 9.2f, 7.3f, 8.3f)
            lineTo(5.8f, 6.4f)
            lineTo(7.4f, 3.6f)
            lineTo(9.9f, 4.1f)
            close()
            moveTo(12f, 9f)
            quadTo(8.9f, 9f, 8.9f, 12f)
            quadTo(8.9f, 15f, 12f, 15f)
            quadTo(15.1f, 15f, 15.1f, 12f)
            quadTo(15.1f, 9f, 12f, 9f)
            close()
        }
    }

    val Food = icon("Food") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(7f, 3f)
            verticalLineTo(11f)
            quadTo(7f, 13f, 9f, 13f)
            verticalLineTo(21f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            quadTo(13f, 13f, 13f, 11f)
            verticalLineTo(3f)
            horizontalLineTo(11.4f)
            verticalLineTo(9f)
            horizontalLineTo(10.2f)
            verticalLineTo(3f)
            horizontalLineTo(8.8f)
            verticalLineTo(9f)
            horizontalLineTo(7.6f)
            verticalLineTo(3f)
            close()
            moveTo(16f, 3f)
            quadTo(20f, 5f, 20f, 11f)
            verticalLineTo(21f)
            horizontalLineTo(18f)
            verticalLineTo(15f)
            horizontalLineTo(15f)
            verticalLineTo(3.6f)
            quadTo(15.4f, 3.2f, 16f, 3f)
            close()
        }
    }

    val Groceries = icon("Groceries") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 8f)
            horizontalLineTo(18f)
            lineTo(16.5f, 21f)
            horizontalLineTo(7.5f)
            close()
            moveTo(9f, 8f)
            quadTo(9f, 4f, 12f, 4f)
            quadTo(15f, 4f, 15f, 8f)
            horizontalLineTo(13f)
            quadTo(13f, 6f, 12f, 6f)
            quadTo(11f, 6f, 11f, 8f)
            close()
        }
    }

    val Travel = icon("Travel") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 14f)
            lineTo(20f, 7f)
            lineTo(21f, 9f)
            lineTo(14f, 15f)
            lineTo(16f, 21f)
            lineTo(14f, 22f)
            lineTo(10f, 17f)
            lineTo(6f, 19f)
            lineTo(5f, 17f)
            lineTo(8f, 14f)
            lineTo(3f, 10f)
            lineTo(4f, 8f)
            close()
        }
    }

    val Bills = icon("Bills") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 3f)
            horizontalLineTo(18f)
            verticalLineTo(21f)
            lineTo(15.5f, 19.5f)
            lineTo(13f, 21f)
            lineTo(10.5f, 19.5f)
            lineTo(8f, 21f)
            lineTo(6f, 19.7f)
            close()
            moveTo(9f, 8f)
            horizontalLineTo(15f)
            verticalLineTo(10f)
            horizontalLineTo(9f)
            close()
            moveTo(9f, 13f)
            horizontalLineTo(15f)
            verticalLineTo(15f)
            horizontalLineTo(9f)
            close()
        }
    }

    val Shopping = icon("Shopping") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 8f)
            horizontalLineTo(18f)
            lineTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(9f, 8f)
            quadTo(9f, 4f, 12f, 4f)
            quadTo(15f, 4f, 15f, 8f)
            horizontalLineTo(13f)
            quadTo(13f, 6f, 12f, 6f)
            quadTo(11f, 6f, 11f, 8f)
            close()
        }
    }

    val Health = icon("Health") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 21f)
            quadTo(5f, 16.5f, 5f, 10f)
            quadTo(5f, 6f, 8.8f, 6f)
            quadTo(10.7f, 6f, 12f, 7.6f)
            quadTo(13.3f, 6f, 15.2f, 6f)
            quadTo(19f, 6f, 19f, 10f)
            quadTo(19f, 16.5f, 12f, 21f)
            close()
        }
    }

    val Salary = icon("Salary") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 6f)
            horizontalLineTo(19f)
            verticalLineTo(18f)
            horizontalLineTo(5f)
            close()
            moveTo(8f, 9f)
            horizontalLineTo(16f)
            verticalLineTo(11f)
            horizontalLineTo(13f)
            quadTo(15f, 13f, 15f, 15f)
            horizontalLineTo(12.5f)
            quadTo(12.5f, 13.6f, 10f, 13f)
            verticalLineTo(11f)
            horizontalLineTo(8f)
            close()
        }
    }

    val Savings = icon("Savings") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4f)
            quadTo(18f, 4f, 20f, 9f)
            horizontalLineTo(22f)
            verticalLineTo(14f)
            horizontalLineTo(20f)
            quadTo(19f, 17f, 16f, 18.5f)
            verticalLineTo(21f)
            horizontalLineTo(13.5f)
            verticalLineTo(19.5f)
            horizontalLineTo(10.5f)
            verticalLineTo(21f)
            horizontalLineTo(8f)
            verticalLineTo(18.5f)
            quadTo(4f, 16.5f, 4f, 12f)
            quadTo(4f, 4f, 12f, 4f)
            close()
            moveTo(15f, 9f)
            quadTo(14f, 8f, 12f, 8f)
            quadTo(10f, 8f, 9f, 9f)
            verticalLineTo(10.5f)
            horizontalLineTo(15f)
            close()
        }
    }

    // Cute kawaii llama/alpaca head — our on-device LLM mascot. Round head, soft ears, big eyes.
    // EvenOdd punches the eyes out as holes.
    val Ai = icon("Ai") {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            // Left ear (soft rounded)
            moveTo(8.7f, 3.6f)
            quadTo(10.2f, 4.2f, 10.3f, 7.4f)
            quadTo(8.1f, 7.4f, 7.4f, 5.4f)
            quadTo(7.2f, 3.6f, 8.7f, 3.6f)
            close()
            // Right ear
            moveTo(15.3f, 3.6f)
            quadTo(13.8f, 4.2f, 13.7f, 7.4f)
            quadTo(15.9f, 7.4f, 16.6f, 5.4f)
            quadTo(16.8f, 3.6f, 15.3f, 3.6f)
            close()
            // Round chubby head
            moveTo(12f, 6.4f)
            quadTo(18.2f, 6.4f, 18.2f, 13.2f)
            quadTo(18.2f, 20.4f, 12f, 20.4f)
            quadTo(5.8f, 20.4f, 5.8f, 13.2f)
            quadTo(5.8f, 6.4f, 12f, 6.4f)
            close()
            // Left eye (big round)
            moveTo(9.7f, 11.6f)
            quadTo(11.1f, 11.6f, 11.1f, 13.4f)
            quadTo(11.1f, 15.2f, 9.7f, 15.2f)
            quadTo(8.3f, 15.2f, 8.3f, 13.4f)
            quadTo(8.3f, 11.6f, 9.7f, 11.6f)
            close()
            // Right eye
            moveTo(14.3f, 11.6f)
            quadTo(15.7f, 11.6f, 15.7f, 13.4f)
            quadTo(15.7f, 15.2f, 14.3f, 15.2f)
            quadTo(12.9f, 15.2f, 12.9f, 13.4f)
            quadTo(12.9f, 11.6f, 14.3f, 11.6f)
            close()
        }
    }

    // Clock — used for the spending history view.
    val History = icon("History") {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(12f, 3f)
            quadTo(21f, 3f, 21f, 12f)
            quadTo(21f, 21f, 12f, 21f)
            quadTo(3f, 21f, 3f, 12f)
            quadTo(3f, 3f, 12f, 3f)
            close()
            // inner hole
            moveTo(12f, 5.6f)
            quadTo(5.6f, 5.6f, 5.6f, 12f)
            quadTo(5.6f, 18.4f, 12f, 18.4f)
            quadTo(18.4f, 18.4f, 18.4f, 12f)
            quadTo(18.4f, 5.6f, 12f, 5.6f)
            close()
            // hands
            moveTo(11f, 7f)
            horizontalLineTo(12.6f)
            verticalLineTo(12f)
            horizontalLineTo(11f)
            close()
            moveTo(12f, 11.2f)
            lineTo(16f, 13.5f)
            lineTo(15.2f, 14.9f)
            lineTo(11.2f, 12.6f)
            close()
        }
    }

    // Pencil — used for inline edit affordances.
    val Edit = icon("Edit") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 20f)
            lineTo(4f, 16.5f)
            lineTo(14.5f, 6f)
            lineTo(18f, 9.5f)
            lineTo(7.5f, 20f)
            close()
            moveTo(15.5f, 5f)
            lineTo(17.5f, 3f)
            quadTo(18.5f, 2f, 19.5f, 3f)
            lineTo(21f, 4.5f)
            quadTo(22f, 5.5f, 21f, 6.5f)
            lineTo(19f, 8.5f)
            close()
        }
    }

    val Other = icon("Other") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4f)
            quadTo(20f, 4f, 20f, 12f)
            quadTo(20f, 20f, 12f, 20f)
            quadTo(4f, 20f, 4f, 12f)
            quadTo(4f, 4f, 12f, 4f)
            close()
            moveTo(8f, 11f)
            horizontalLineTo(16f)
            verticalLineTo(13f)
            horizontalLineTo(8f)
            close()
        }
    }
}

fun iconForCategory(kind: CategoryKind): ImageVector = when (kind) {
    CategoryKind.Food -> MochiIcons.Food
    CategoryKind.Groceries -> MochiIcons.Groceries
    CategoryKind.Travel -> MochiIcons.Travel
    CategoryKind.Bills -> MochiIcons.Bills
    CategoryKind.Shopping -> MochiIcons.Shopping
    CategoryKind.Health -> MochiIcons.Health
    CategoryKind.Salary -> MochiIcons.Salary
    CategoryKind.Savings -> MochiIcons.Savings
    CategoryKind.Other -> MochiIcons.Other
}

private inline fun icon(
    name: String,
    crossinline builder: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply { builder() }.build()
