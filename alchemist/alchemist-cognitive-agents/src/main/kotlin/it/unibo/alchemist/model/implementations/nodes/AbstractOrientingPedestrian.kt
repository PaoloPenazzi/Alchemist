package it.unibo.alchemist.model.implementations.nodes

import it.unibo.alchemist.model.implementations.graph.builder.NavigationGraphBuilder
import it.unibo.alchemist.model.implementations.graph.builder.addEdge
import it.unibo.alchemist.model.implementations.geometry.liesBetween
import it.unibo.alchemist.model.implementations.graph.containsDestination
import it.unibo.alchemist.model.implementations.graph.destinationsWithin
import it.unibo.alchemist.model.implementations.graph.isReachable
import it.unibo.alchemist.model.implementations.graph.primMST
import it.unibo.alchemist.model.implementations.graph.dijkstraShortestPath
import it.unibo.alchemist.model.implementations.utils.shuffled
import it.unibo.alchemist.model.interfaces.Position
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.PedestrianGroup
import it.unibo.alchemist.model.interfaces.OrientingPedestrian
import it.unibo.alchemist.model.interfaces.geometry.ConvexGeometricShape
import it.unibo.alchemist.model.interfaces.geometry.GeometricTransformation
import it.unibo.alchemist.model.interfaces.geometry.Vector
import it.unibo.alchemist.model.interfaces.graph.GraphEdge
import it.unibo.alchemist.model.interfaces.graph.NavigationGraph
import org.apache.commons.math3.random.RandomGenerator

/**
 * An abstract orienting pedestrian, defining an algorithm capable of generating
 * a [cognitiveMap], provided a [NavigationGraph] describing the environment.
 * The creation of landmarks is left to subclasses via factory method, see
 * [generateLandmarkWithin].
 *
 * @param T the concentration type.
 * @param P the [Position] type and [Vector] type for the space this pedestrian is inside.
 * @param A the transformations supported by the shapes in this space.
 * @param N the type of landmarks in the pedestrian's [cognitiveMap].
 * @param M the type of nodes of the [environmentGraph].
 * @param F the type of edges of the [environmentGraph].
 *
 * The algorithm produces a cognitive map whose edges are simple [GraphEdge]s, this means
 * no extra information regarding the connection of landmarks is stored in the cognitive map
 * (a part from the boolean information concerning the fact a connection is present of course).
 */
abstract class AbstractOrientingPedestrian<
    T,
    P,
    A : GeometricTransformation<P>,
    N : ConvexGeometricShape<P, A>,
    M : ConvexGeometricShape<P, A>,
    F : GraphEdge<M>
>(
    final override val knowledgeDegree: Double,
    /**
     * The random generator to use in order to preserve reproducibility.
     */
    protected val randomGenerator: RandomGenerator,
    /**
     * A navigation graph describing the environment. Nodes are [ConvexGeometricShape]s
     * that should represent the walkable areas of the environment (i.e. the areas that
     * are freely traversable by agents). Edges represent connections between these
     * areas. Additionally, a [NavigationGraph] can store some destinations which
     * will be considered as possible final destinations by this pedestrian.
     */
    protected val environmentGraph: NavigationGraph<P, A, M, F>,
    environment: Environment<T, P>,
    group: PedestrianGroup<T>? = null,
    /*
     * When generating the cognitive map, the regions whose diameter is
     * < of this quantity * the diameter of the agent will be discarded
     * and no landmark will be generated inside them.
     */
    private val minArea: Double = 10.0
) : OrientingPedestrian<T, P, A, N, GraphEdge<N>>,
    HomogeneousPedestrianImpl<T, P>(environment, randomGenerator, group) where P : Position<P>, P : Vector<P> {

    init {
        require(knowledgeDegree.liesBetween(0.0, 1.0)) { "knowledge degree must be in [0,1]" }
    }

    override val volatileMemory: MutableMap<in ConvexGeometricShape<P, A>, Int> = HashMap()

    /*
     * Here's a brief description of how the algorithm operates:
     * We randomly select a % of environment's regions equal to the knowledge 
     * degree of the pedestrian, we then generate a landmark in each of them
     * (via factory method). Concerning the connections between landmarks, we
     * produce a graph in which each landmark is connected to any other landmark
     * reachable from it, with an edge whose weight depends on the number of
     * rooms that need to be traversed (information on reachability between regions
     * and number of areas to be traversed can be obtained from the environment's
     * graph). We then produce a minimum spanning tree of the described graph.
     */
    override val cognitiveMap: NavigationGraph<P, A, N, GraphEdge<N>> by lazy {
        val builder = NavigationGraphBuilder<P, A, N, GraphEdge<N>>()
        /*
         * The rooms in which landmarks will be placed.
         */
        val rooms = environmentGraph.nodes()
            .filter { it.diameter > shape.diameter * minArea || environmentGraph.containsDestination(it) }
            .shuffled(randomGenerator)
            .toList()
            .takePercentage(knowledgeDegree)
            .toMutableList()
        /*
         * At least one destination is provided if knowledge degree >= 0.1
         */
        if (rooms.none { environmentGraph.containsDestination(it) } && knowledgeDegree >= 0.1) {
            environmentGraph.nodes()
                .firstOrNull { environmentGraph.containsDestination(it) }
                ?.let { rooms.add(it) }
        }
        val landmarks = rooms.map { generateLandmarkWithin(it) }
        /*
         * Maps each landmark's index to the indices of the ones reachable from it
         */
        val reachability = rooms.indices
            .map { i ->
                i to rooms.indices.filter { j -> i != j && environmentGraph.isReachable(rooms[i], rooms[j]) }
            }.toMap()
        reachability.forEach {
            it.value.forEach { i -> builder.addEdge(landmarks[it.key], landmarks[i]) }
        }
        builder.build(rooms.flatMap { environmentGraph.destinationsWithin(it) })
            .primMST {
                environmentGraph.dijkstraShortestPath(
                    rooms[landmarks.indexOf(it.tail)],
                    rooms[landmarks.indexOf(it.head)],
                    { 1.0 }
                )?.weight ?: it.tail.centroid.getDistanceTo(it.head.centroid)
            }
    }

    /**
     * Generates a landmark entirely contained in the given region.
     * If such region contains one or more destinations, the generated
     * landmark must contain at least one of them.
     */
    protected abstract fun generateLandmarkWithin(region: M): N

    private fun <E> List<E>.takePercentage(percentage: Double) = subList(0, (percentage * size).toInt())
}
