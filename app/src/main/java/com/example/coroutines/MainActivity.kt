package com.example.coroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), CoroutineScope {
    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun create() {
        job = Job()
    }

    fun destroy() {
        job.cancel()
    }

    fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }

    private val i: Int = 61
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        when (i) {
            //basics
            0 -> basiccoroutine()
            1 -> blockingNonBlocking1()
            2 -> blockingNonBlocking2()
            3 -> withJob()
            4 -> Structuredconcurrency()
            5 -> Scopebuilder()
            6 -> functionrefactoring()
            7 -> areLightWeight()
            8 -> longRunningCoroutine()

            //cancellation and timeout
            9 -> cancellingCoroutineExecution()
            10 -> cancellationCooperative()
            11 -> computationCodeCancellable()
            12 -> closingWithFinally()
            13 -> nonCancellableBlock()
            14 -> timeout()

            //channel
            15 -> channelBasics()
            16 -> closeIterationOver()
            17 -> buildingChannelProducers()
            18 -> pipelines()
            19 -> primenumbers()
            //multiple coroutines receive from the same channel
            20 -> fanOut()
            //Multiple coroutines send to the same channel
            21 -> fanIn()
            22 -> bufferedChannel()
            // They are served in first-in first-out order
            23 -> channelsAreFair()
            //       //Ticker channel can be used in select to perform "on tick" action
            24 -> tickerChannel()

            //Composing suspending functions // its like remote service call
            25 -> sequentialByDefault()
            26 -> concurrentAsync()
            27 -> lazilyStartedAsync() //using optional start parameter
            28 -> asyncStyleFunctions() //Kotlin coroutines is strongly discouraged(logical error)
            29 -> structuredConcurrencyWithAsync()  //any child throws an exception then other child cancelled


            //Coroutine context and dispatchers
            30 -> dispatchersAndThreads()
            31 -> unconfinedConfinedDispatcher()
            32 -> debuggingCoroutinesAndThreads()
            33 -> jumpingBetweenThreads()
            34 -> jobInContext()
            35 -> childrenOfcoroutine()
            36 -> parentalResponsibilities()
            //user specified name of coroutine
            37 -> namingCoroutinesForDebugging() // same as debuggingCoroutinesAndThreads(32)
            38 -> combiningContextElements()
            39 -> cancelationViaExplicitJob()
            40 -> threadLocalData()

            //Exception handling
            41 -> exceptionPropagation()
            42 -> coroutineExceptionHandler()
            43 -> cancellationAndExceptions1() //parent can cancel child without canceling it(parent)
            44 -> cancellationAndExceptions2()
            45 -> exceptionsAggregation1()
            46 -> exceptionsAggregation2()
            47 -> supervisionJobs()
            48 -> supervisionScope()
            49 -> exceptionsInSupervisedCoroutines()

            //Select expression (experimental)
            50 -> selectFromChannel()
            51 -> selectingOnClose()
            52 -> selectingToSend()
            53 -> selectingDeferredValues()
            54 -> switchMapDeferreds()

            //Shared mutable state and concurrency
            55 -> theProblem()
            56 -> volatilesAreOfNoHelp()
            57 -> threadSafeDataStructure() // faster solution
            58 -> threadConfinementFineGrained() //This code works very slowly
            59 -> threadConfinementCoarseGrained() //thread confinement is performed in large chunks
                                                   // This now works much faster and produces correct result.
            60 -> mutualExclusion() //  The key difference is that Mutex.lock() is a suspending function.
                                    // It does not block a thread.
            61 -> actorDemo()

        }
    }

    val mutex = Mutex()
    val counterContext = newSingleThreadContext("CounterContext")
    var counter1 = AtomicInteger()
    var counter = 0

    // Message types for counterActor
    sealed class CounterMsg {
        object IncCounter : CounterMsg() // one-way message to increment counter
        class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply
    }

    private fun actorDemo() = runBlocking<Unit> {
        val counter = counterActor() // create the actor
        GlobalScope.massiveRun {
            counter.send(CounterMsg.IncCounter)
        }
        // send a message to get a counter value from an actor
        val response = CompletableDeferred<Int>()
        counter.send(CounterMsg.GetCounter(response))
        println("Counter = ${response.await()}")
        counter.close() // shutdown the actor
    }



    // This function launches a new counter actor
    fun CoroutineScope.counterActor() = actor<CounterMsg> {
        var counter = 0 // actor state
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is CounterMsg.IncCounter -> counter++
                is CounterMsg.GetCounter -> msg.response.complete(counter)
            }
        }
    }

    private fun mutualExclusion() = runBlocking<Unit> {
        GlobalScope.massiveRun {
            mutex.withLock {
                counter++
            }
        }
        println("Counter = $counter")
    }

    private fun threadConfinementCoarseGrained() = runBlocking<Unit> {
        CoroutineScope(counterContext).massiveRun {
            // run each coroutine in the single-threaded context
            counter++
        }
        println("Counter = $counter")
    }

    private fun threadConfinementFineGrained() = runBlocking<Unit> {
        GlobalScope.massiveRun {
            // run each coroutine with DefaultDispathcer
            withContext(counterContext) {
                // but confine each increment to the single-threaded context
                counter++
            }
        }
        println("Counter = $counter")
    }

    private fun threadSafeDataStructure() = runBlocking<Unit> {
        GlobalScope.massiveRun {
            counter1.incrementAndGet()
        }
        println("Counter = ${counter1.get()}")
    }

    private fun volatilesAreOfNoHelp() = runBlocking<Unit> {
        GlobalScope.massiveRun {
            counter1.incrementAndGet()
        }
        println("Counter = $counter")
    }

    private fun theProblem() = runBlocking<Unit> {
        GlobalScope.massiveRun {
            counter++
        }
        println("Counter = $counter")

        //reproduce the problem
        /* CoroutineScope(mtContext).massiveRun { // use it instead of Dispatchers.Default in this sample and below
             counter++
         }
         println("Counter = $counter")*/
    }

    suspend fun CoroutineScope.massiveRun(action: suspend () -> Unit) {
        val n = 100  // number of coroutines to launch
        val k = 1000 // times an action is repeated by each coroutine
        val time = measureTimeMillis {
            val jobs = List(n) {
                launch {
                    repeat(k) { action() }
                }
            }
            jobs.forEach { it.join() }
        }
        println("Completed ${n * k} actions in $time ms")
    }


    private fun switchMapDeferreds() = runBlocking<Unit> {
        val chan = Channel<Deferred<String>>() // the channel for test
        launch {
            // launch printing coroutine
            for (s in switchMapDeferreds(chan))
                println(s) // print each received string
        }
        chan.send(asyncString("BEGIN", 100))
        delay(200) // enough time for "BEGIN" to be produced
        chan.send(asyncString("Slow", 500))
        delay(100) // not enough time to produce slow
        chan.send(asyncString("Replace", 100))
        delay(500) // give it time before the last one
        chan.send(asyncString("END", 500))
        delay(1000) // give it time to process
        chan.close() // close the channel ...
        delay(500) // and wait some time to let it finish
    }

    fun CoroutineScope.switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
        var current = input.receive() // start with first received deferred value
        while (isActive) { // loop while not cancelled/closed
            val next = select<Deferred<String>?> {
                // return next deferred value from this select or null
                input.onReceiveOrNull { update ->
                    update // replaces next value to wait
                }
                current.onAwait { value ->
                    send(value) // send value that current deferred has produced
                    input.receiveOrNull() // and use the next deferred from the input channel
                }
            }
            if (next == null) {
                println("Channel was closed")
                break // out of loop
            } else {
                current = next
            }
        }
    }

    fun CoroutineScope.asyncString(str: String, time: Long) = async {
        delay(time)
        str
    }

    private fun selectingDeferredValues() = runBlocking<Unit> {
        val list = asyncStringsList()
        val result = select<String> {
            list.withIndex().forEach { (index, deferred) ->
                deferred.onAwait { answer ->
                    "Deferred $index produced answer '$answer'"
                }
            }
        }
        println(result)
        val countActive = list.count { it.isActive }
        println("$countActive coroutines are still active")
    }

    fun CoroutineScope.asyncString(time: Int) = async {
        delay(time.toLong())
        "Waited for $time ms"
    }

    fun CoroutineScope.asyncStringsList(): List<Deferred<String>> {
        val random = Random(3)
        return List(12) { asyncString(random.nextInt(1000)) }
    }

    fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
        for (num in 1..10) { // produce 10 numbers from 1 to 10
            delay(100) // every 100 ms
            select<Unit> {
                onSend(num) {} // Send to the primary channel
                side.onSend(num) {} // or to the side channel
            }
        }
    }

    private fun selectingToSend() = runBlocking<Unit> {
        val side = Channel<Int>() // allocate side channel
        launch {
            // this is a very fast consumer for the side channel
            side.consumeEach { println("Side channel has $it") }
        }
        produceNumbers(side).consumeEach {
            println("Consuming $it")
            delay(250) // let us digest the consumed number properly, do not hurry
        }
        println("Done consuming")
        coroutineContext.cancelChildren()
    }

    suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
            select<String> {
                a.onReceiveOrNull { value ->
                    if (value == null)
                        "Channel 'a' is closed"
                    else
                        "a -> '$value'"
                }
                b.onReceiveOrNull { value ->
                    if (value == null)
                        "Channel 'b' is closed"
                    else
                        "b -> '$value'"
                }
            }

    private fun selectingOnClose() = runBlocking<Unit> {
        val a = produce<String> {
            repeat(4) { send("Hello $it") }
        }
        val b = produce<String> {
            repeat(4) { send("World $it") }
        }
        repeat(8) {
            // print first eight results
            println(selectAorB(a, b))
        }
        coroutineContext.cancelChildren()
    }

    fun CoroutineScope.fizz() = produce<String> {
        while (true) { // sends "Fizz" every 300 ms
            delay(300)
            send("Fizz")
        }
    }

    fun CoroutineScope.buzz() = produce<String> {
        while (true) { // sends "Buzz!" every 500 ms
            delay(500)
            send("Buzz!")
        }
    }

    suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
        select<Unit> {
            // <Unit> means that this select expression does not produce any result
            fizz.onReceive { value ->
                // this is the first select clause
                println("fizz -> '$value'")
            }
            buzz.onReceive { value ->
                // this is the second select clause
                println("buzz -> '$value'")
            }
        }
    }

    private fun selectFromChannel() = runBlocking<Unit> {
        val fizz = fizz()
        val buzz = buzz()
        repeat(7) {
            selectFizzBuzz(fizz, buzz)
        }
        coroutineContext.cancelChildren() // cancel fizz & buzz coroutines
    }

    private fun exceptionsInSupervisedCoroutines() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        supervisorScope {
            val child = launch(handler) {
                println("Child throws an exception")
                throw AssertionError()
            }
            println("Scope is completing")
        }
        println("Scope is completed")
    }

    private fun supervisionScope() = runBlocking {
        try {
            supervisorScope {
                val child = launch {
                    try {
                        println("Child is sleeping")
                        delay(Long.MAX_VALUE)
                    } finally {
                        println("Child is cancelled")
                    }
                }
                // Give our child a chance to execute and print using yield
                yield()
                println("Throwing exception from scope")
                throw AssertionError()
            }
        } catch (e: AssertionError) {
            println("Caught assertion error")
        }
    }

    private fun supervisionJobs() = runBlocking {
        val supervisor = SupervisorJob()
        with(CoroutineScope(coroutineContext + supervisor)) {
            // launch the first child -- its exception is ignored for this example (don't do this in practice!)
            val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
                println("First child is failing")
                throw AssertionError("First child is cancelled")
            }
            // launch the second child
            val secondChild = launch {
                firstChild.join()
                // Cancellation of the first child is not propagated to the second child
                println("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    // But cancellation of the supervisor is propagated
                    println("Second child is cancelled because supervisor is cancelled")
                }
            }
            // wait until the first child fails & completes
            firstChild.join()
            println("Cancelling supervisor")
            supervisor.cancel()
            secondChild.join()
        }
    }

    private fun exceptionsAggregation2() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught original $exception")
        }
        val job = GlobalScope.launch(handler) {
            val inner = launch {
                launch {
                    launch {
                        throw IOException()
                    }
                }
            }
            try {
                inner.join()
            } catch (e: CancellationException) {
                println("Rethrowing CancellationException with original cause")
                throw e
            }
        }
        job.join()
    }

    private fun exceptionsAggregation1() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception with suppressed ${exception.suppressed.contentToString()}")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    throw ArithmeticException()
                }
            }
            launch {
                delay(100)
                throw IOException()
            }
            delay(Long.MAX_VALUE)
        }
        job.join()
    }

    private fun cancellationAndExceptions2() = runBlocking<Unit> {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                // the first child
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    withContext(NonCancellable) {
                        println("Children are cancelled, but exception is not handled until all children terminate")
                        delay(100)
                        println("The first child finished its non cancellable block")
                    }
                }
            }
            launch {
                // the second child
                delay(10)
                println("Second child throws an exception")
                throw ArithmeticException()
            }
        }
        job.join()
    }

    private fun cancellationAndExceptions1() = runBlocking<Unit> {
        val job = launch {
            val child = launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    println("Child is cancelled")
                }
            }
            yield()
            println("Cancelling child")
            child.cancel()
            child.join()
            yield()
            println("Parent is not cancelled")
        }
        job.join()
    }

    private fun coroutineExceptionHandler() = runBlocking<Unit> {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            throw AssertionError()
        }
        val deferred = GlobalScope.async(handler) {
            throw ArithmeticException() // Nothing will be printed, relying on user to call deferred.await()
        }
        joinAll(job, deferred)
    }

    private fun exceptionPropagation() = runBlocking {
        val job = GlobalScope.launch {
            println("Throwing exception from launch")
            throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
        }
        job.join()
        println("Joined failed job")
        val deferred = GlobalScope.async {
            println("Throwing exception from async")
            throw ArithmeticException() // Nothing is printed, relying on user to call await
        }
        try {
            deferred.await()
            println("Unreached")
        } catch (e: ArithmeticException) {
            println("Caught ArithmeticException")
        }
    }

    val threadLocal = ThreadLocal<String?>() // declare thread-local variable

    private fun threadLocalData() = runBlocking<Unit> {
        threadLocal.set("main")
        println("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
            println("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
            yield()
            println("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        }
        job.join()
        println("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    }

    private fun cancelationViaExplicitJob() = runBlocking<Unit> {
        val activity = MainActivity()
        activity.create() // create an activity
        activity.doSomething() // run test function
        println("Launched coroutines")
        delay(500L) // delay for half a second
        println("Destroying activity!")
        activity.destroy() // cancels all coroutines
        delay(1000) // visually confirm that they don't work
    }

    private fun combiningContextElements() = runBlocking<Unit> {
        launch(Dispatchers.Default + CoroutineName("test")) {
            println("I'm working in thread ${Thread.currentThread().name}")
        }
    }

    private fun namingCoroutinesForDebugging() = runBlocking<Unit> {
        log("Started main coroutine")
        // run two background value computations
        val v1 = async(CoroutineName("v1coroutine")) {
            delay(500)
            log("Computing v1")
            252
        }
        val v2 = async(CoroutineName("v2coroutine")) {
            delay(1000)
            log("Computing v2")
            6
        }
        log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
    }

    private fun parentalResponsibilities() = runBlocking<Unit> {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            repeat(3) { i ->
                // launch a few children jobs
                launch {
                    delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                    println("Coroutine $i is done")
                }
            }
            println("request: I'm done and I don't explicitly join my children that are still active")
        }
        request.join() // wait for completion of the request, including all its children
        println("Now processing of the request is complete")
    }

    private fun childrenOfcoroutine() = runBlocking<Unit> {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            // it spawns two other jobs, one with GlobalScope
            GlobalScope.launch {
                println("job1: I run in GlobalScope and execute independently!")
                delay(1000)
                println("job1: I am not affected by cancellation of the request")
            }
            // and the other inherits the parent context
            launch {
                delay(100)
                println("job2: I am a child of the request coroutine")
                delay(1000)
                println("job2: I will not execute this line if my parent request is cancelled")
            }
        }
        delay(500)
        request.cancel() // cancel processing of the request
        delay(1000) // delay a second to see what happens
        println("main: Who has survived request cancellation?")
    }

    private fun jobInContext() = runBlocking<Unit> {
        println("My Job is ${coroutineContext[Job]}")
    }

    private fun jumpingBetweenThreads() {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")
                    withContext(ctx2) {
                        log("Working in ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    private fun debuggingCoroutinesAndThreads() = runBlocking<Unit> {

        val a = async {
            log("I'm computing a piece of the answer")
            6
        }
        val b = async {
            log("I'm computing another piece of the answer")
            7
        }
        log("The answer is ${a.await() * b.await()}")
    }

    private fun unconfinedConfinedDispatcher() = runBlocking {
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
        }
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
        }
    }

    private fun dispatchersAndThreads() = runBlocking<Unit> {
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Default) {
            // will get dispatched to DefaultDispatcher
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
        }
    }

    private fun structuredConcurrencyWithAsync() = runBlocking {
        val time = measureTimeMillis {
            println("The answer is ${concurrentSum()}")
        }
        println("Completed in $time ms")
    }

    suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        //async coroutines builder is defined as extension on CoroutineScope we need to have it in the scope
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }

    private fun asyncStyleFunctions() = runBlocking<Unit> {
        val time = measureTimeMillis {
            // we can initiate async actions outside of a coroutine
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            // but waiting for a result must involve either suspending or blocking.
            // here we use `runBlocking { ... }` to block the main thread while waiting for the result
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")
    }

    // The result type of somethingUsefulOneAsync is Deferred<Int>
    fun somethingUsefulOneAsync() = GlobalScope.async {
        doSomethingUsefulOne()
    }

    // The result type of somethingUsefulTwoAsync is Deferred<Int>
    fun somethingUsefulTwoAsync() = GlobalScope.async {
        doSomethingUsefulTwo()
    }

    private fun lazilyStartedAsync() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            // some computation
            one.start() // start the first one
            two.start() // start the second one
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    private fun concurrentAsync() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    private fun sequentialByDefault() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("Completed in $time ms")

    }

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }

    private fun tickerChannel() = runBlocking {
        val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // create ticker channel
        var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Initial element is available immediately: $nextElement") // initial delay hasn't passed yet

        nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements has 100ms delay
        println("Next element is not ready in 50 ms: $nextElement")

        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 100 ms: $nextElement")

        // Emulate large consumption delays
        println("Consumer pauses for 150ms")
        delay(150)
        // Next element is available immediately
        nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Next element is available immediately after large consumer delay: $nextElement")
        // Note that the pause between `receive` calls is taken into account and next element arrives faster
        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

        tickerChannel.cancel() // indicate that no more elements are needed
    }

    data class Ball(var hits: Int)

    private fun channelsAreFair() = runBlocking {
        val table = Channel<Ball>() // a shared table
        launch { player("ping", table) }
        launch { player("pong", table) }
        table.send(Ball(0)) // serve the ball
        delay(1000) // delay 1 second
        coroutineContext.cancelChildren() // game over, cancel them
    }

    suspend fun player(name: String, table: Channel<Ball>) {
        for (ball in table) { // receive the ball in a loop
            ball.hits++
            println("$name $ball")
            delay(300) // wait a bit
            table.send(ball) // send the ball back
        }
    }

    private fun bufferedChannel() = runBlocking {
        val channel = Channel<Int>(4) // create buffered channel
        val sender = launch {
            // launch sender coroutine
            repeat(10) {
                println("Sending $it") // print before sending each element
                // The first four elements are added to the buffer and the sender suspends when trying to send the fifth one.
                channel.send(it) // will suspend when buffer is full
            }
        }
        // don't receive anything... just wait....
        delay(1000)
        sender.cancel() // cancel sender coroutine
    }

    private fun fanIn() = runBlocking {
        val channel = Channel<String>()
        launch { sendString(channel, "foo", 200L) }
        launch { sendString(channel, "BAR!", 500L) }
        repeat(6) {
            // receive first six
            println(channel.receive())
        }
        coroutineContext.cancelChildren() // cancel all children to let main finish
    }

    suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
        while (true) {
            delay(time)
            channel.send(s)
        }
    }

    private fun fanOut() = runBlocking {
        val producer = produceNumber()
        repeat(5) { launchProcessor(it, producer) }
        delay(950)
        producer.cancel()
    }

    fun CoroutineScope.produceNumber() = produce<Int> {
        var x = 1 // start from 1
        while (true) {
            send(x++) // produce next
            delay(100) // wait 0.1s
        }
    }

    fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
        for (msg in channel) {
            println("Processor #$id received $msg")
        }
    }

    private fun primenumbers() = runBlocking {
        var cur = numbersFrom(2)
        for (i in 1..10) {
            val prime = cur.receive()
            println(prime)

            cur = filter(cur, prime)

        }
        coroutineContext.cancelChildren()
    }

    fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
        var x = start
        while (true) send(x++) // infinite stream of integers from start
    }

    fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
        for (x in numbers) if (x % prime != 0) send(x)
    }

    private fun pipelines() = runBlocking {
        val numbers = produceNumbers() // produces integers from 1 and on
        val squares = square(numbers) // squares integers
        for (i in 1..5) println(squares.receive()) // print first five
        println("Done!") // we are done

        coroutineContext.cancelChildren() // cancel children coroutines // if no childern
    }

    fun CoroutineScope.produceNumbers() = produce<Int> {
        var x = 1
        while (true) send(x++) // infinite stream of integers starting from 1
    }

    fun CoroutineScope.square(number: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
        for (x in number) send(x * x)
    }

    private fun buildingChannelProducers() = runBlocking {
        val square = produceSquares()
        square.consumeEach { println(it) }
        println("Done")
    }

    fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
        for (i in 1..5) send(i * i)
    }

    private fun closeIterationOver() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5) channel.send(x * x)
            channel.close() // we're done sending
        }
        // here we print received values using `for` loop (until the channel is closed)
        for (y in channel) println(y)
        println("Done!")
    }

    private fun channelBasics() = runBlocking {
        val channel = Channel<Int>()
        launch { for (x in 1..5) channel.send(x * x) }
        repeat(5) { println(channel.receive()) }
        println("done")
    }

    private fun timeout() = runBlocking {
        val result = withTimeoutOrNull(1300L)
        {

            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
            "Done"
        }
        println("Result is $result")
    }


    private fun nonCancellableBlock() = runBlocking {
        //since all well-behaving closing operations (closing a file, cancelling a job, or closing any kind of a communication channel) are usually non-blocking and do not involve any suspending functions. However, in the rare case when you need to suspend in the cancelled coroutine you can wrap the corresponding code in withContext(NonCancellable) {...}

        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) {
                    println("I'm running finally")
                    delay(1000L)
                    println("And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    private fun closingWithFinally() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    private fun computationCodeCancellable() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (isActive) { // cancellable computation loop
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    private fun cancellationCooperative() = runBlocking {
        //if a coroutine is working in a computation and does not check for cancellation, then it cannot be cancelled
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) { // computation loop, just wastes CPU
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    private fun cancellingCoroutineExecution() = runBlocking {
        //The launch function returns a Job that can be used to cancel running coroutine
        val job = launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancel() // cancels the job
        job.join() // waits for job's completion
        println("main: Now I can quit.")
    }

    private fun longRunningCoroutine() = runBlocking {
        GlobalScope.launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // just quit after delay
    }

    private fun areLightWeight() = runBlocking {
        repeat(100_000) {
            // launch a lot of coroutines
            launch {
                delay(1000L)
                print(".")
            }
        }
    }

    private fun functionrefactoring() = runBlocking {
        launch { doWorld() }
        println("Hello,")
    }

    private suspend fun doWorld() {
        delay(1000L)
        println("World!")
    }

    private fun Scopebuilder() = runBlocking {
        launch {
            delay(200L)
            println("Task from runBlocking")
        }

        coroutineScope {
            // Creates a new coroutine scope
            launch {
                delay(500L)
                println("Task from nested launch")
            }

            delay(100L)
            println("Task from coroutine scope") // This line will be printed before nested launch
        }

        println("Coroutine scope is over") // This line is not printed until nested launch completes
    }

    private fun Structuredconcurrency() = runBlocking {
        launch {
            // launch new coroutine in the scope of runBlocking
            delay(1000L)
            println("World!")
        }
        println("Hello,")
    }

    private fun withJob() = runBlocking {
        val job = GlobalScope.launch {
            // launch new coroutine and keep a reference to its Job
            delay(1000L)
            println("World!")
        }
        println("Hello,")
        job.join() // wait until child coroutine completes
    }

    private fun blockingNonBlocking2() = runBlocking {
        GlobalScope.launch {
            // launch new coroutine in background and continue
            delay(1000L)
            println("World!")
        }
        println("Hello,") // main coroutine continues here immediately
        delay(2000L)      // delaying for 2 seconds to keep JVM alive
    }

    private fun blockingNonBlocking1() {
        GlobalScope.launch {
            // launch new coroutine in background and continue
            delay(1000L)
            println("World!")
        }
        println("Hello,") // main thread continues here immediately
        runBlocking {
            // but this expression blocks the main thread
            delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive
        }
    }

    private fun basiccoroutine() {
        GlobalScope.launch {
            // launch new coroutine in background and continue
            delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
            println("World!") // print after delay
        }
        println("Hello,") // main thread continues while coroutine is delayed
        Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive
    }

    /* private fun main(){
        *//* GlobalScope.launch {
            delay(1000L)
            println("world")
        }
        println("Hello,") // main thread continues while coroutine is delayed
        Thread.sleep(2000L)*//*

        //block non block
        *//*GlobalScope.launch { // launch new coroutine in background and continue
            delay(1000L)
            println("World!")
        }
        println("Hello,") // main thread continues here immediately
        runBlocking {     // but this expression blocks the main thread
            delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive
        }*//*

        //job
        val job = GlobalScope.launch { // launch new coroutine and keep a reference to its Job
            delay(1000L)
            println("World!")
        }
        println("Hello,")
        job.join() // wait until child coroutine completes
    }*/

    /*fun main() = runBlocking { // this: CoroutineScope
        launch {
            delay(200L)
            println("Task from runBlocking")
        }

        coroutineScope { // Creates a new coroutine scope
            launch {
                delay(500L)
                println("Task from nested launch")
            }

            delay(100L)
            println("Task from coroutine scope") // This line will be printed before nested launch
        }

        println("Coroutine scope is over") // This line is not printed until nested launch completes
    }*/

    fun main() = runBlocking {
        val job = GlobalScope.launch {
            repeat(1000) { i ->

                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L)
        println("main: I'm tired of waiting!")
        job.cancel() // cancels the job
        job.join() // waits for job's completion
        println("main: Now I can quit.")
    }
}
