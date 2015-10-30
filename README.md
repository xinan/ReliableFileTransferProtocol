# ReliableFileTransferProtocol

Simple sending and receiving transport level code for implementing a reliable file transfer protocol over UDP. The underlying channel is unreliable and may corrupt, drop or even re-order the packets at random. 

### Speed
**First Place** in CS2105 (Introduction to Computer Networks) Speed Contest AY15/16 Sem1. 

(1.835s for 50MB, 2% dropping rate, 2% corruption rate, on grader's machine)

Note that some code are specially tuned for the speed contest and thus do not follow good practices.
