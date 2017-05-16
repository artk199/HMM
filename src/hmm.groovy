import static groovy.json.JsonOutput.*

def states =        ['motor','car','autosan']
def observations =   ['S','M','X']

/*
    Transition matrix
    Dla każdego z pojazdów prawdopodobienstwo jaki przyjedzie nastepny.
 */
def transitionM = [
        'motor':    ['motor':0.5,'car':0.4,'autosan':0.1],
        'car':      ['motor':0.1,'car':0.6,'autosan':0.3],
        'autosan':  ['motor':0.01,'car':0.29,'autosan':0.7]
]

/*
    Emission probabilities
    Dla każdego z pojazdów wykorzystanie wody(S = small, M = medium, X = large).
 */
def observationM = [
        'motor':    ['S':0.8,'M':0.1,'X':0.1],
        'car':      ['S':0.3,'M':0.4,'X':0.3],
        'autosan':  ['S':0.05,'M':0.15,'X':0.8]
]

//Observed sequence 1 = S, 2 = M, 3 = X
def observS = ['S','S','S','S','S']

//Initial matrix
def initial = ['motor':0.0,'car':0.0,'autosan':1.0]


def forward(states,observations,statesM,transitionM,observationM,observS){

    def p = []
    def stateLen = states.size()
    def sM = statesM.clone()

    //Dla kazdej obserwacji
    observS.eachWithIndex{ def o, int i ->
        def newstates = sM.clone()
        stateLen.times{ int j ->
            def val = 0
            stateLen.times { int n ->

                val += sM[states[n]] * transitionM[states[n]][states[j]]
            }
            newstates[states[j]] = val
        }
        sM = newstates.clone()

        sM.each{ def key,value ->
            sM[key] *= observationM[key][o]
        }

        p << (sM)
    }

    return p
}

println(prettyPrint(toJson(forward(states,observations,initial,transitionM,observationM,observS))))
