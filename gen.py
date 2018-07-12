import random
for i in range(0,200):
    sx=int(random.random()*80)
    sy=int(random.random()*80)
    dx=int(random.random()*80)
    dy=int(random.random()*80)
    print(str(int(i/2))+'=[CR,('+str(sx)+','+str(sy)+'),('+str(dx)+','+str(dy)+')]')
    
