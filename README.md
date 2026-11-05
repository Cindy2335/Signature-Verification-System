Signature forgery is a serious and costly issue in South Africa. Banks, SARS, the Deeds Office, and legal firms still rely on handwritten signatures to authorise transactions and documents.

Manual verification is slow, inconsistent, and prone to human error — especially under time pressure.

The system automates this verification process using graph-based image analysis, with no machine learning or third-party AI libraries — only data structures that are self implemented.

## Screenshots


<img src= "Screenshots/Screenshot 2026-11-03 232652.png" width=500>
<img src= "Screenshots/Screenshot 2026-11-03 232754.png" width=500>
<img src= "Screenshots/Screenshot 2026-11-03 232818.png" width=500>
<img src= "Screenshots/Screenshot 2026-11-03 232841.png" width=500>
<img src= "Screenshots/Screenshot 2026-11-03 233518.png" width=500>





## How To Use The System

1. Click `Load Reference`.
2. Choose a signature image.
3. Click `Load Candidate`.
4. Choose another signature image.
5. Click `Compare` to get a similarity score and accept/reject decision.
6. Click `Reference Path` or `Candidate Path` to highlight the longest graph path.
7. Click `Analyze Graphs` to view graph counts and structural metrics.

Supported image formats:

* `.png`
* `.jpg`
* `.jpeg`
* `.bmp`
* `.pgm`
* `.pbm`
* `.ppm`

## Algorithm Summary

1. **Image preprocessing:** Grayscale conversion, Otsu thresholding and cropping.
2. **Skeletonisation:** Zhang-Suen thinning.
3. **Graph construction:** Endpoints, junctions and weighted stroke-chain edges.
4. **Graph comparison:** Topology, degree histograms, edge lengths, shortest-path profiles and node geometry.
5. **Pathfinding:** Dijkstra's algorithm.

   ## Technologies and Concepts
- Java
- Graph Theory
- Image Processing
- Otsu Thresholding
- Zhang-Suen Thinning Algorithm
- Dijkstra's Shortest Path Algorithm
- Object-Oriented Programming
- Self-Implemented Data Structures
   


Datasets: [**https://www.kaggle.com/datasets/pranjitlahon/signatures**](https://www.kaggle.com/datasets/pranjitlahon/signatures)


