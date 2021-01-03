package ships

import area.Coords

open class DeckBase(val deckNo: Int, var isHit: Boolean)

class Deck(deckNo: Int, isHit: Boolean, val coords: Coords): DeckBase(deckNo, isHit)