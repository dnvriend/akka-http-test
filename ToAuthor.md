# Misc notes

I struggled to get the simple act of converting a `HttpResponse` (when received through a connection chain, i.e. `Source.single(...)` - not the `Http().singleRequest(..)` approach) to convert to the type I need.

Did not find a place where your sample does this, though obviously it does, somewhere.

Anyways, this didn't work (though it compiles):

```
// This gives: "Cannot cast akka.http.scaladsl.model.HttpResponse to scala.collection.Seq"
//
resp_fut.mapTo[T]
```

This did:

```
resp_fut.flatMap( (resp: HttpResponse) =>
  Unmarshal(resp.entity).to[T]
)
```

If you know what's up, I'd like to hear. If you can embed a sample that shows this clearly somewhere, that'd be great for others tossing away hours with the `akka-http` multitude of degrees of freedom.
