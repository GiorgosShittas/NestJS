import {AfterInsert,AfterRemove,AfterUpdate,Entity, Column, PrimaryGeneratedColumn} from 'typeorm'

@Entity()
export class User{
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    email: string
    
    @Column()
    password: string

    @AfterInsert()
    logInsert(){
        console.log("Inserted a user with ID: ",this.id)
    }
    @AfterUpdate()
    logUpdate(){
        console.log("Updated a user with ID: ",this.id)
    }
    @AfterRemove()
    logRemove(){
        console.log("Removed a user with ID: ",this.id)
    }
}