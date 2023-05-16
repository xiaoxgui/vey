import { Component, OnInit} from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface File {
  name: string;
  lastModified: string;
  size: number;
  message: string;
}

interface DeleteBucketResponse {
  message: string;
}
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  title: string='angular';
  buckets: string[] = [];
  bucketName: string = '';
  files: File[] = [];
  selectedBucket: string = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.listBuckets();
    this.getFiles();
  }
  formatSize(size: number): string {
    if (size < 1024) {
      return size + ' B';
    } else if (size < 1024 * 1024) {
      return (size / 1024).toFixed(2) + ' KB';
    } else if (size < 1024 * 1024 * 1024) {
      return (size / 1024 / 1024).toFixed(2) + ' MB';
    } else {
      return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB';
    }
  }
  listBuckets() {
    this.http.get<{ buckets: string[] }>('http://localhost:8080/listBuckets').subscribe(response => {
      this.buckets = response.buckets;
    });
  }
  createBucket() {
    this.http.post(`http://localhost:8080/createBucket?bucketName=${this.bucketName}`, {}).subscribe(response => {
      console.log(response);
      this.listBuckets();
    });
  }

  deleteBucket(bucketName: string) {
    this.http.post<DeleteBucketResponse>(`http://localhost:8080/deleteBucket?bucketName=${bucketName}`, null).subscribe(response => {
      if (response.message === '删除成功') {
        this.buckets = this.buckets.filter(bucket => bucket !== bucketName);
      } else {
        alert(response.message);
      }
    });
  }


  uploadFile(event: any, bucketName: string) {
    const file = event.target.files[0];
    const formData = new FormData();
    formData.append('file', file);
    formData.append('bucketName', bucketName);

    this.http.post('http://localhost:8080/upload', formData).subscribe(
      (response: any) => {
        console.log(response);
        alert(response.message);
        this.getFiles();
      },
      error => {
        console.log(error);
        alert(error.error.message);
      }
    );
  }

  getFiles() {
    if (this.selectedBucket) {
      this.http.get<File[]>(`http://localhost:8080/list?bucketName=${this.selectedBucket}`).subscribe(
        data => {
          this.files = data;
        },
        error => {
          console.log(error);
        }
      );
    }
  }

  downloadFile(name: string) {
    this.http.get(`http://localhost:8080/download/${name}?bucketName=${this.selectedBucket}`, { responseType: 'blob' }).subscribe(
      data => {
        const url = window.URL.createObjectURL(data);
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        link.click();
      },
      error => {
        console.log(error);
      }
    );
  }

  deleteFile(name: string) {
    this.http.delete(`http://localhost:8080/delete/${name}?bucketName=${this.selectedBucket}`, { responseType: 'text' }).subscribe(
      data => {
        console.log(data);
        this.getFiles(); // 重新获取文件列表
      },
      error => {
        console.log(error);
      }
    );
  }

}
