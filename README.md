Signature forgery is a serious and costly issue in South Africa. Banks, SARS, the Deeds Office, and legal firms still rely on handwritten signatures to authorise transactions and documents.

Manual verification is slow, inconsistent, and prone to human error — especially under time pressure.

The system automates this verification process using graph-based image analysis, with no machine learning or third-party AI libraries — only data structures that are self implemented.



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

1. Convert the image to grayscale.
2. Use Otsu thresholding to separate ink from background.
3. Crop the image to the ink area.
4. Skeletonize the signature with Zhang-Suen thinning.
5. Convert the skeleton into a graph:

   * degree 1 pixels become endpoints
   * degree 3+ pixels become junctions
   * stroke chains become weighted edges
6. Compare two graphs using topology counts, graph degree histogram, normalized
edge length profile, endpoint shortest-path profile, and normalized node
geometry.
7. Use Dijkstra pathfinding to find shortest paths and the longest
endpoint-to-endpoint flow path.


Datasets: [**https://www.kaggle.com/datasets/pranjitlahon/signatures**](https://www.kaggle.com/datasets/pranjitlahon/signatures)
