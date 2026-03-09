# <div align="center">OAUTH Implementation</div>

## Purpose:
#### *Make onboarding very easy*.</br> 
From previous projects, I've realised people are not willing to sign up for something
unless it's something they actively use. Now this could be due to two potential factors; either they are just not
bothered sitting through entering their email and filling 2 fields with their password + other fields, or they 
are simply reluctant for security reasons. Implementing the easiest onboarding process, doesn't guarantee retention,
it simply increases the likelihood of it. Nonetheless, it's a critical aspect to include in modern day sites and apps
as it has a massive potential to generate more signups and improves overall security by going passwordless etc..

---
## Considerations:
- Generally passwords aren't changeable if logged in with social login, e.g github or google.
In practice, it may be doable, we could let user change password so that they are able to
sign in with both the social login + the registered email and password aswell.
However, assuming somebody (actually most singups will probably be using oauth) sings in with
google for example, they will most likely never even contemplate fiddling around with a password. This is a quite
a chunk of complexity and thinking we will have to handle for a possibility that has a very low chance
of occuring. No stats to back this, just a reasonable inference. In other words, the ROI isn't very appealing here.
I will infact hold my horses on this one. Changing passwords logic is mainly reserved for those with email + pass 
signups.

- What if somebody signs in with github using an email e.g johndoe@gmail.com and then tries to signup with google
which is using the same johndoe@gmail.com. In the case this happens, we should detect the duplicate email and then
automatically link to google provider instead of letting two accounts be created.
