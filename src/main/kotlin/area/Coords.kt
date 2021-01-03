package area

import helpers.toPreviousCurrentAndNext

data class Coords(val x:Int, val y:Int){
    fun safetyCoords(): List<Coords>{
        val ret = mutableListOf<Coords>()
        val xCoords = toPreviousCurrentAndNext(this.x)
        val yCoords = toPreviousCurrentAndNext(this.y)
        for(x in xCoords)
            for(y in yCoords)
                ret.add(Coords(x, y))
        return ret
    }
}