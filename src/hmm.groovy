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
def observS = ['S','X','S','X','S','X','S','X','S','X']

//Initial matrix
def initial = ['motor':0.4,'osobowy':0.3,'autobus':0.3]


def forward(states,observations,statesM,transitionM,observationM,observS){
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
        sM.each{ def key,value ->
            sM[key] *= observationM[key][o]
        }
        p << sM
    }
    return p
}

def backward(states,observations,statesM,transitionM,observationM,observS){
    def bwd = [:]
    def oLength = observS.size()
    states.each{ state ->
        bwd[state] = new double[oLength]
        bwd[state][oLength-1] = 1
    }
    for(int i=oLength-2;i>=0;i--){
        states.each{ s1 ->
            bwd[s1][i] = 0
            states.each{ s2 ->
                bwd[s1][i] += bwd[s2][i + 1] * transitionM[s1][s2] * observationM[s2][observS[i+1]]
            }
        }
    }
    return bwd
}

def forwardBackward(states,observations,statesM,transitionM,observationM,observS){
    def f = forward(states,observations,statesM,transitionM,observationM,observS)
    def b = backward(states,observations,statesM,transitionM,observationM,observS)
    println prettyPrint(toJson(f))
    println prettyPrint(toJson(b))
    //scale
    f.each { it ->
        double sum = 0
        states.each{ state ->
            sum += it[state]
        }
        states.each{ state ->
            sum += it[state]
        }
    }
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
    println "Prob: " + valmax
    return argmax;
}

println(prettyPrint(toJson(forwardBackward(states,observations,initial,transitionM,observationM,observS))))
println(virtebi(observations,states,initial,transitionM,observationM,observS))
