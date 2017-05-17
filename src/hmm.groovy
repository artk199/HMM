import static groovy.json.JsonOutput.*

def states =        ['motor','osobowy','autobus']
def observations =   ['S','M','X']

/*
    Transition matrix
    Dla każdego z pojazdów prawdopodobienstwo jaki przyjedzie nastepny.
 */
def transitionM = [
        'motor':    ['motor':0.5,'osobowy':0.4,'autobus':0.1],
        'osobowy':      ['motor':0.1,'osobowy':0.6,'autobus':0.3],
        'autobus':  ['motor':0.05,'osobowy':0.25,'autobus':0.7]
]

/*
    Emission probabilities
    Dla każdego z pojazdów wykorzystanie wody(S = small, M = medium, X = large).
 */
def observationM = [
        'motor':    ['S':0.8,'M':0.1,'X':0.1],
        'osobowy':      ['S':0.3,'M':0.4,'X':0.3],
        'autobus':  ['S':0.05,'M':0.15,'X':0.8]
]

//Observed sequence 1 = S, 2 = M, 3 = X
def observS = ['S','S','M','X','S','S','M','X','X','X']

//Initial matrix
def initial = ['motor':0.4,'osobowy':0.3,'autobus':0.3]


def forward(states,observations,statesM,transitionM,observationM,observS,scale){
    def p = []
    def sM = statesM.clone()

    observS.each{ def o ->
        def newstates = sM.clone()
        states.each{ s1 ->
            def val = 0
            states.each{ s2 ->
                val += sM[s2] * transitionM[s2][s1]
            }
            newstates[s1] = val
        }
        sM = newstates
        def sum = 0
        sM.each{ def key,value ->
            sM[key] *= observationM[key][o]
            sum += sM[key]
        }

        scale << 1/sum
        sM.each{ key,value ->
        //    sM[key] = value/sum
        }
        p << sM
    }
    return p
}

def backward(states,observations,statesM,transitionM,observationM,observS,scale){
    def oLength = observS.size()
    def bwd = new Object[oLength]
    bwd.eachWithIndex{it,i->
        bwd[i] = [:]
    }
    states.each{ state ->
        bwd[oLength-1][state] = 1
    }
    for(int i=oLength-2;i>=0;i--){
        states.each{ s1 ->
            bwd[i][s1] = 0
            states.each{ s2 ->
                bwd[i][s1] += bwd[s2][i + 1] * transitionM[s1][s2] * observationM[s2][observS[i+1]]
            }
            //bwd[i][s1] *= scale[i]
        }
    }
    return bwd
}

def forwardBackward(states,observations,statesM,transitionM,observationM,observS){
    def scales = []
    def f = forward(states,observations,statesM,transitionM,observationM,observS,scales)
    def b = backward(states,observations,statesM,transitionM,observationM,observS,scales)
    def result = []
    def ret = []
    observS.eachWithIndex { def entry, int i ->
        def denom = 0
        states.each{ state ->
            //denom += statesM[state]* observationM[state][observS[0]]*b[0][state]
        }
        def tmp = [:]
        states.each{ state ->
                tmp[state] = f[i][state] * b[i][state]///denom
        }
        result << tmp

        def argmax
        def valmax = 0;
        states.eachWithIndex{ state, k ->
            def v_prob = tmp[state]
            if (v_prob > valmax) {
                argmax = state
                valmax = v_prob;
            }
        }
        ret << argmax
    }
    //println prettyPrint(toJson(f))
    //println prettyPrint(toJson(b))
    //println "result: " + prettyPrint(toJson(result))
    return ret

}

public def virtebi(observations, states, statesM, transitionM, observationM, observS) {

    def nodes = [:]
    states.each{ s ->
        nodes[s] = [path:[s], prob:statesM[s] * observationM[s][observS[0]]]
    }

    def steps = []
    steps[0] = nodes;

    for(int i=1; i<observS.size(); i++){
        def maxNode = [:]
        def prevNodes = steps[i-1]
        states.each{ nextState ->
            def maxPath = []
            def max = 0
            states.each { state ->
                double prob = observationM[nextState][observS[i]] * transitionM[state][nextState] * prevNodes[state].prob
                if (prob > max) {
                    max = prob
                    maxPath = prevNodes[state].path.collect()
                    if (maxPath.size() < observS.size())
                        maxPath << nextState
                }
            }
            maxNode[nextState] = [path:maxPath, prob:max]
        }
        steps << maxNode
    }
    //println(prettyPrint(toJson(steps)))
    nodes = steps[observS.size()-1]

    def argmax = new int[0];
    def valmax = 0;
    states.eachWithIndex{ state, i ->
        def v_path = nodes[state].path
        def v_prob = nodes[state].prob
        if (v_prob > valmax) {
            argmax = v_path
            valmax = v_prob;
        }
    }
    //println "Prob: " + valmax
    return argmax;
}

def test(states,observations,initial,transitionM,observationM){
    for(int k=2;k<=100;k++) {
        def vsum = 0
        def fbsum = 0
        100.times {
            def testData = [observS: [], vert: [], fb: [], init: []]
            //randomuj pierwszy element
            Random random = new Random()
            testData.init += randState(initial, random)
            for (int i = 1; i < k; i++) {
                testData.init << randState(transitionM[testData.init[i - 1]], random)
            }
            testData.init.each { it2 ->
                testData.observS << randState(observationM[it2], random)
            }

            testData.fb = forwardBackward(states, observations, initial, transitionM, observationM, testData.observS)
            testData.vert = virtebi(observations, states, initial, transitionM, observationM, testData.observS)
            //println prettyPrint(toJson(testData))
            vsum += getDiff(testData.init, testData.vert)
            fbsum += getDiff(testData.init, testData.fb)
        }
        println ""+k+"\t" + vsum/100 + "\t" +fbsum/100
    }

}

def randState(dist,Random random){
    def rand = random.nextDouble()
    def sum = 0
    def ret = null
    dist.each{ key,value ->
        if(rand >= sum && rand <= (sum+value)){
            ret = key
        }
        sum += value
    }
    return ret
}
test(states,observations,initial,transitionM,observationM)
//println(forwardBackward(states,observations,initial,transitionM,observationM,observS))
//println(virtebi(observations,states,initial,transitionM,observationM,observS))
def getDiff(def a1, def a2) {
    int count = 0
    a1.eachWithIndex{ it, i ->
        if(it != a2[i])
            count ++
    }
    return count
}