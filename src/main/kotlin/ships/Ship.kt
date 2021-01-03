package ships

import area.Coords

data class ShipBase(val size: Int)

data class Ship(val size: Int, val isHorizontal: Boolean, val topLeftCoords: Coords) {

    constructor(ship: ShipBase, isHorizontal: Boolean, topLeftCoords: Coords):
            this(ship.size, isHorizontal, topLeftCoords)

    val decks: List<DeckBase> =
            IntProgression.fromClosedRange(0,size-1,1).map{DeckBase(it,false)}

    val decksCoorded: List<Deck> = decks.map{
            if(isHorizontal)
                Deck(it.deckNo, it.isHit, topLeftCoords.copy(x = topLeftCoords.x + it.deckNo))
            else
                Deck(it.deckNo, it.isHit, topLeftCoords.copy(y = topLeftCoords.y + it.deckNo))
        }

    val safetyCoords: List<Coords> = decksCoorded.map{it.coords.safetyCoords()}.flatten().toSet().toList()

    val decksCoords: List<Coords> = decksCoorded.map{it.coords}

    override fun toString(): String {
        return "size: ${size}, isHorizontal: ${isHorizontal}, coords:${topLeftCoords}"
    }
}
