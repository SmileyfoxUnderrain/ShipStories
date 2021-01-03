package helpers

fun<A, B, C> Iterable<A>.cartesianProduct(other: Iterable<B>, combiner:(a: A, b: B) -> C): Iterable<C>{
    return this.flatMap { t -> other.map{o -> combiner(t, o)}}
}