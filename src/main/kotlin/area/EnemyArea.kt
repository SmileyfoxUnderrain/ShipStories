package area

import ships.Ship
import ships.Deck

class EnemyArea(xSize: Int, ySize: Int): Area(xSize, ySize) {
    val hitDecks = mutableListOf<Deck>()
    private val hitObjectsSafetyCoords = mutableSetOf<Coords>()

    val untouchedCells: List<Coords>
        get() = allMapCoords
                .minus(hitDecks.map { it.coords })
                .minus(hitObjectsSafetyCoords)
                .minus(missedHitsCoords)

    val occupiedCells: List<Coords>
        get() = hitDecks.map{ it.coords}
            .plus(hitObjectsSafetyCoords)
            .plus(missedHitsCoords)

    val hitDecksAndShipDecks: List<Deck>
        get() = hitDecks.plus(placedShips.flatMap { it.decksCoorded})

    fun addHitDeck(deck: Deck): Boolean {
        if(hitDecks
                .map{ it.coords }
                .plus(placedShips.flatMap{it.decksCoorded}.map{ it.coords })
                .contains(deck.coords))
            return false
        hitDecks.add(deck)
        val neighboringDecks = findNeighboringHitDecks(deck)

        if(neighboringDecks.any()){
            //Add to hit objects safety coords new coords, calculated of current hit deck and neighboring hit decks
            val vCoords = neighboringDecks
                .map { it.coords }
                .filter { it.y == deck.coords.y }
                .map{c -> listOf(
                    c.copy(y = c.y - 1)
                    ,c.copy(y = c.y + 1)
                    ,deck.coords.copy(y = deck.coords.y - 1)
                    ,deck.coords.copy(y = deck.coords.y + 1)
                )}.flatten()
            val hCoords = neighboringDecks
                .map { it.coords }
                .filter { it.x == deck.coords.x }
                .map{c -> listOf(
                    c.copy(x = c.x - 1)
                    ,c.copy(x = c.x + 1)
                    ,deck.coords.copy(x = deck.coords.x - 1)
                    ,deck.coords.copy(x = deck.coords.x + 1)
                )}.flatten()
            hitObjectsSafetyCoords.addAll(vCoords.filter { it.y == -1 })
            hitObjectsSafetyCoords.addAll(hCoords.filter { it.x == -1 })
        }
        return true
    }
    fun findNeighboringHitDecks(deck: Deck): Iterable<Deck>{
        val neighboringCoords = listOf(
            deck.coords.copy(x = deck.coords.x - 1)
            ,deck.coords.copy(x = deck.coords.x + 1)
            ,deck.coords.copy(y = deck.coords.y - 1)
            ,deck.coords.copy(y = deck.coords.y + 1)
        )
        val ret = hitDecks.filter { neighboringCoords.contains(it.coords) }
        return ret
    }

    fun addDefeatedShip(ship: Ship){
        if(placeShip(ship)) {
            for(d in ship.decks)
                d.isHit = true
            hitObjectsSafetyCoords.addAll(ship.safetyCoords)
            hitDecks.removeIf { ship.decksCoorded.map{d -> d.coords}.contains(it.coords) }
        }
    }

    override fun printCellsInPseudographic(){
        val hitDecksCoords = hitDecks.map { it.coords }
        println("   ${(IntProgression.fromClosedRange(0, xSize - 1, 1)
                .joinToString(separator = "") { "|${it}|" })}")
        for(y in 0 until xSize){
            print(" $y ")
            for(x in 0 until ySize) {
                val currentCoords = Coords(x, y)
                var currentCell = "|_|"


                if(hitObjectsSafetyCoords.contains(currentCoords))
                    currentCell = "|~|"
                if(hitDecksCoords.contains(currentCoords))
                    currentCell = "|H|"
                if(missedHitsCoords.contains(currentCoords))
                    currentCell = "|O|"
                if(placedShips.flatMap { it.decksCoords }.contains(currentCoords))
                    currentCell = "|X|"

                print(currentCell)
            }
            println()
        }
    }
}