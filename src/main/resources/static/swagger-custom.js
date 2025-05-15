async function uploadFileFromInput() {
    const fileInput = document.getElementById('uploadButton');
    if (fileInput.files.length > 0) {
        const file = fileInput.files[0];
        uploadFile(file);
    } else {
        alert('Please select a file to upload.');
    }
}

async function uploadFile(file) {
    console.log("Start");
    const token = 'eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJBRE1JTiJdLCJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzMxMzg2Mjg5LCJleHAiOjE3MzE0MjIyODl9.hn8w5BGKYN-xapUmnvJcSxk_ND3T7R_tjOA_YWy10b0'; // Replace this with the actual token or fetch dynamically

    // Step 1: Fetch presigned URLs and uploadId from the backend
    const response = await fetch(`http://localhost:8080/api/upload/generate-presigned-urls?fileName=${file.name}&fileSize=${file.size}`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok) {
        alert('Failed to get presigned URLs');
        return;
    }

    const data = await response.json();
    const presignedUrls = data.presignedUrls;
    const uploadId = data.uploadId; // Get uploadId from the response

    // Step 2: Divide the file into chunks and upload each part
    const chunkSize = 5 * 1024 * 1024; // 5MB
    const completedParts = [];
    const uploadPromises = [];
    for (let i = 0; i < presignedUrls.length; i++) {
        const chunk = file.slice(i * chunkSize, (i + 1) * chunkSize);

        const uploadPromise = fetch(presignedUrls[i], {
            method: 'PUT',
            body: chunk
        }).then(uploadResponse => {
            if (uploadResponse.ok) {
                const eTag = uploadResponse.headers.get('ETag');
                completedParts.push({ etag: eTag.slice(1, -1), partNumber: i + 1 });
            } else {
                throw new Error(`Failed to upload part ${i + 1}`);
            }
        }).catch(error => {
            console.error('Error during file upload:', error);
            throw new Error(`Failed to upload part ${i + 1}: ${error.message}`);
        });

        uploadPromises.push(uploadPromise);
    }

    // Step 3: Notify the backend to complete the multipart upload
    try {

        await Promise.all(uploadPromises);
        console.log("File name:", file.name);
        console.log("UploadId:", uploadId);
        console.log("Completed Parts:", completedParts);

        const completeResponse = await fetch(`http://localhost:8080/api/upload/complete-multipart-upload?fileName=${file.name}&uploadId=${uploadId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(completedParts)
        });

        if (completeResponse.ok) {
            console.log("END");
            alert('File uploaded successfully');
        } else {
            alert('Failed to complete multipart upload');
        }
    } catch (error) {
        console.error('Error during multipart upload completion:', error);
        alert('Multipart upload completion failed');
    }
}

// Add an event listener to the file input
document.getElementById('uploadButton').addEventListener('change', (event) => {
    uploadFileFromInput();
});