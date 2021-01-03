package area

import helpers.cartesianProduct
import ships.Ship
import ships.Deck


open class Area(val xSize: Int, val ySize: Int){
    private val ships: MutableList<Ship> = mutableListOf()

    val placedShips: List<Ship>
        get() = ships

    fun placeShip(ship: Ship): Boolean{
        if(ship.decksCoords.any{it.x >= xSize} || ship.decksCoords.any{ it.y >= ySize }) {
            println("$ship is not placed because it crosses the boards of area")
            return false
        }
        if(shipsCoords.intersect(ship.safetyCoords).any()) {
            println("$ship is not placed because it intersects with other ships' safety areas")
            return false
        }

        ships.add(ship)
        return true
    }

    val allMapCoords = mutableListOf<Coords>()

    init{
            val allXCoords = IntProgression.fromClosedRange(0, xSize -1, 1)
            val allYCoords = IntProgression.fromClosedRange(0, ySize -1, 1)
            val ret = allXCoords.cartesianProduct(allYCoords) { x, y -> Coords(x, y) }
            allMapCoords.addAll(ret)
        }

    val placesForShip: List<FreePlaceForShip>
        get() {
            val freeCells = allMapCoords.minus(ships.flatMap { it.safetyCoords }.toSet())
            val ret = freeCells.map{
                FreePlaceForShip(
                        it,
                        countFreeCells(it, freeCells, true),
                        countFreeCells(it, freeCells, false))
            }
            return ret
        }
    val shipsCoords: List<Coords>
        get() = ships.flatMap { it.decksCoorded }.map { it.coords }

    val missedHitsCoords: MutableSet<Coords> = mutableSetOf()

    fun getDeckOnCoordsOrNull(coords: Coords): Deck?{
        return ships.flatMap { it.decksCoorded }.firstOrNull{it.coords == coords}
    }

    fun getDestroyedShipOrNull(coords: Coords): Ship?{
        return ships.firstOrNull { it.decksCoorded.all { deck -> deck.isHit } && it.decksCoords.contains(coords) }
    }

    fun clear() = ships.clear()

    private fun countFreeCells(currentCoords: Coords, freeCells: Iterable<Coords>, isHorizontal: Boolean): Int{
        var count = 1
        var havePrevious = true
        while(havePrevious){
            if(freeCells.any{it == currentCoords.copy(
                            x = if(isHorizontal) currentCoords.x + count else currentCoords.x,
                            y = if(!isHorizontal) currentCoords.y + count else currentCoords.y)})
                count++
            else
                havePrevious = false
        }
        return count
    }

    open fun printCellsInPseudographic(){
        val hitCoords = ships.flatMap { it.decksCoorded }.filter{it.isHit}.map{it.coords}
        val unHitCoords = ships.flatMap { it.decksCoorded }.filter{!it.isHit}.map{it.coords}
        val safetyAreaCoords = ships.flatMap { it.safetyCoords }
        println("   ${(IntProgression.fromClosedRange(0, xSize - 1, 1)
                .joinToString(separator = "") { "|${it}|" })}")
        for(y in 0 until xSize){
            print(" $y ")
            for(x in 0 until ySize) {
                val currentCoords = Coords(x, y)
                var currentCell = "|_|"

                if(safetyAreaCoords.contains(currentCoords))
                    currentCell = "|~|"

                if(unHitCoords.contains(currentCoords))
                    currentCell = "|W|"

                if(hitCoords.contains(currentCoords))
                    currentCell = "|X|"

                if(missedHitsCoords.contains(currentCoords))
                    currentCell = "|O|"
                print(currentCell)
            }
            println()
        }
    }
}
