# TxtImportControllerApi

All URIs are relative to *http://localhost:8080*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**importarDesdeTxt**](#importardesdetxt) | **POST** /api/importar/txt | |

# **importarDesdeTxt**
> object importarDesdeTxt()


### Example

```typescript
import {
    TxtImportControllerApi,
    Configuration,
    ImportarDesdeTxtRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new TxtImportControllerApi(configuration);

let importarDesdeTxtRequest: ImportarDesdeTxtRequest; // (optional)

const { status, data } = await apiInstance.importarDesdeTxt(
    importarDesdeTxtRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **importarDesdeTxtRequest** | **ImportarDesdeTxtRequest**|  | |


### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

