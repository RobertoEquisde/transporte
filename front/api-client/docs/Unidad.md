# Unidad


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **number** |  | [optional] [default to undefined]
**noSerie** | **string** |  | [optional] [default to undefined]
**modelo** | [**Modelo**](Modelo.md) |  | [optional] [default to undefined]
**comentario** | **string** |  | [optional] [default to undefined]
**origen** | **string** |  | [optional] [default to undefined]
**debisFecha** | **string** |  | [optional] [default to undefined]
**distribuidor** | [**Distribuidor**](Distribuidor.md) |  | [optional] [default to undefined]
**reportadoA** | **string** |  | [optional] [default to undefined]
**pagoDistribuidora** | **string** |  | [optional] [default to undefined]
**valorUnidad** | **number** |  | [optional] [default to undefined]
**seguros** | [**Array&lt;Seguro&gt;**](Seguro.md) |  | [optional] [default to undefined]
**cobros** | [**Array&lt;Cobros&gt;**](Cobros.md) |  | [optional] [default to undefined]

## Example

```typescript
import { Unidad } from './api';

const instance: Unidad = {
    id,
    noSerie,
    modelo,
    comentario,
    origen,
    debisFecha,
    distribuidor,
    reportadoA,
    pagoDistribuidora,
    valorUnidad,
    seguros,
    cobros,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
