package logic.hitCalculators

import area.Coords
import area.EnemyArea
import logic.Navy
import ships.ShipBase

class ProbCalculator(val edgeCorrection: Double = 1.2) : HitCalculator {

    private val undestroyedShips = mutableListOf<ShipBase>()

    override fun calculateHit(enemyArea: EnemyArea, navy: Navy): Coords {
        undestroyedShips.clear()
        undestroyedShips.addAll(detectUndestroyedShips(enemyArea, navy))
        val evaluatedCells = evaluateCells(enemyArea)
        val cellsNearHitDecks = evaluatedCells.filter{ it.closeHitDecks.any()}

        if(cellsNearHitDecks.any()){
            val grouppedCells = cellsNearHitDecks.groupBy{it.closeHitDecks.map{d -> d.steps}.minOf { s -> s }}
            val minStepsToHitDeck = grouppedCells.keys.minOf { it }
            val cellsToHit = grouppedCells[minStepsToHitDeck]!!
            if(minStepsToHitDeck == 1)
                cellsToHit.forEach { c ->
                    val direction = c.closeHitDecks.filter{it.steps == 1}.first().direction
                    c.closeEmptyCells.removeIf { it.direction != direction }
                }
            val cellToHit = cellsToHit.maxByOrNull { it.correctedValue }!!
            return cellToHit.coords
        }
        val grouppedCells = evaluatedCells.groupBy{it.correctedValue}
        val cellToHit = grouppedCells[grouppedCells.keys.maxOf { it }]!!.random()
        return cellToHit.coords

    }
    private fun evaluateCells(enemyArea: EnemyArea): List<EvaluatedCell>{
        val evaluatedCells = mutableListOf<EvaluatedCell>()
        for(coords in enemyArea.untouchedCells)
            evaluatedCells.add(evaluateCell(coords, enemyArea))

        return evaluatedCells
    }

    private fun detectUndestroyedShips(enemyArea: EnemyArea, navy: Navy): List<ShipBase>{
        val undestroyedShips = navy.ships.toMutableList()
        for(placedShip in enemyArea.placedShips){
            if(navy.ships.any{it.size == placedShip.size})
                undestroyedShips.remove(undestroyedShips.first { it.size == placedShip.size })
        }
        return undestroyedShips
    }

    private fun evaluateCell(coords: Coords, enemyArea: EnemyArea): EvaluatedCell{
        val cell = EvaluatedCell(coords, edgeCorrection)
        for(direction in Direction.values())
            evaluateCellRecursive(cell, coords, direction, enemyArea)
        return cell
    }

    private fun evaluateCellRecursive(cell: EvaluatedCell, coords: Coords, direction: Direction, enemyArea: EnemyArea){
        val maxUndestroyedShipSize = undestroyedShips.maxOfOrNull { it.size }!!
        if( maxUndestroyedShipSize == 1) {
            cell.closeEmptyCells.add(EmptyCellOnDirection(coords, direction))
            return
        }
        val hitDecksCoords = enemyArea.hitDecks.map { it.coords }
        val length = getLengthInclusive(cell, coords, direction)

        if(undestroyedShips.map{it.size}.contains(length))
            cell.closeEmptyCells.add(EmptyCellOnDirection(coords, direction))

        if(hitDecksCoords.contains(coords))
            if(length <= maxUndestroyedShipSize)
                cell.closeHitDecks.add(HitDeckOnDirection(coords, length, direction))
            // Иначе проверить, нет ли предыдущей ячейки или ячеек, связанных с группой данных Decks
            // и удалить лишние
            else if(cell.closeHitDecks.any{ it.direction == direction && it.steps == length - 1 }) {
                var stepBack = 1
                while(cell.closeHitDecks.any{ it.direction == direction && it.steps == length - stepBack })
                    stepBack++
                while(stepBack > 1) {
                    cell.closeHitDecks.removeIf { it.direction == direction && it.steps == length - stepBack }
                    stepBack--
                }
            }

        val nextCoords = getNextCoordsOrNull(coords, direction, enemyArea) ?: return

        if(length < maxUndestroyedShipSize || hitDecksCoords.contains(nextCoords))
            evaluateCellRecursive(cell, nextCoords, direction, enemyArea)
    }
    private fun getLengthInclusive(cell: EvaluatedCell, coords: Coords, direction: Direction): Int{
        if(direction == Direction.LEFT)
            return cell.coords.x - coords.x + 1
        if(direction == Direction.UP)
            return cell.coords.y - coords.y + 1
        if(direction == Direction.RIGHT)
            return coords.x - cell.coords.x + 1
        if(direction == Direction.DOWN)
            return coords.y - cell.coords.y + 1
        throw IllegalArgumentException("No such direction: $direction")
    }

    private fun getNextCoordsOrNull(coords: Coords, direction: Direction, enemyArea: EnemyArea): Coords?{
        var retCoords: Coords? = null

        if(direction == Direction.LEFT)
            retCoords = coords.copy(x = coords.x - 1)
        if(direction == Direction.UP)
            retCoords = coords.copy(y = coords.y - 1)
        if(direction == Direction.RIGHT)
            retCoords = coords.copy(x = coords.x + 1)
        if(direction == Direction.DOWN)
            retCoords = coords.copy(y = coords.y + 1)
        if(retCoords == null)
            throw IllegalArgumentException("No such direction: $direction")

        if(retCoords.x !in 0..9 || retCoords.y !in 0..9)
            return null

        val hitCoords = enemyArea.hitObjectsSafetyCoords.plus(enemyArea.missedHitsCoords)
        if(hitCoords.contains(retCoords))
            return null

        return retCoords
    }
}
class EvaluatedCell( val coords: Coords, val edgeCorrection: Double = 1.2){
    val closeEmptyCells = mutableListOf<EmptyCellOnDirection>()
    val closeHitDecks = mutableListOf<HitDeckOnDirection>()

    val correctedValue: Double
        get(){
            return if(coords.x == 0 || coords.x == 9 || coords.y == 0 || coords.y == 9)
                closeEmptyCells.size * edgeCorrection
            else
                closeEmptyCells.size.toDouble()
        }
}
class EmptyCellOnDirection(val coords: Coords, val  direction: Direction)
class HitDeckOnDirection(val coords: Coords, val steps: Int, val direction: Direction)

enum class Direction{LEFT, UP, RIGHT, DOWN}